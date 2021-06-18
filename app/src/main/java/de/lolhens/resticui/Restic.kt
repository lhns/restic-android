package de.lolhens.resticui

import android.content.Context
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.net.InetAddress
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture

interface ResticStorage {
    companion object {
        fun fromContext(context: Context): ResticStorage = object : ResticStorage {
            private val _lib = File(context.applicationInfo.nativeLibraryDir)
            override fun lib(): File = _lib
            override fun cache(): File = context.cacheDir
            override fun storage(): List<File> =
                listOf(
                    File("/storage")
                ).filter { it.exists() }
        }
    }

    fun lib(): File
    fun cache(): File
    fun storage(): List<File>
}

data class ResticException(val exitCode: Int, val stderr: List<String>) :
    Exception("Restic error $exitCode:\n${stderr.joinToString("\n")}")

class Restic(
    private val storage: ResticStorage
) {
    private val proot = storage.lib().resolve("proot")
    private val restic = storage.lib().resolve("restic")
    private val loader = storage.lib().resolve("loader")
    private val loader32 = storage.lib().resolve("loader32")

    private fun binds(hostsFile: File): List<Pair<String, String>> = listOf(
        Pair("/system", "/system"),
        Pair("/data", "/data"),
        Pair(hostsFile.absolutePath, "/etc/hosts"),
    ).plus(storage.storage().map {
        val path = it.absolutePath
        Pair(path, path)
    })

    private fun args(binds: List<Pair<String, String>>): List<String> =
        listOf(
            proot.absolutePath,
        ).plus(
            binds.flatMap { (from, to) -> listOf("-b", "$from:$to") }
        ).plus(
            listOf(
                "--kill-on-exit",
                restic.absolutePath
            )
        )

    private fun vars(): List<Pair<String, String>> = listOf(
        Pair("PATH", "/system/bin"),
        Pair("LD_LIBRARY_PATH", storage.lib().absolutePath),
        Pair("PROOT_LOADER", loader.absolutePath),
        Pair("PROOT_LOADER_32", loader32.absolutePath),
        Pair("PROOT_TMP_DIR", storage.cache().absolutePath),
        Pair("RESTIC_CACHE_DIR", storage.cache().resolve("restic").absolutePath)
    )

    private fun hostsFile(hosts: List<String>): CompletableFuture<File> =
        CompletableFuture.supplyAsync {
            val hostsFileContent = hosts.map { host ->
                val address = InetAddress.getByName(host)
                "${address.hostAddress} $host"
            }.joinToString("\n")

            val hostsFile = File.createTempFile("hosts", "", storage.cache())
            try {
                hostsFile.writeText(hostsFileContent, StandardCharsets.UTF_8)
                hostsFile
            } catch (e: Exception) {
                hostsFile.delete()
                throw e
            }
        }

    fun restic(
        args: List<String>,
        vars: List<Pair<String, String>> = emptyList(),
        hosts: List<String> = emptyList(),
        filterOut: ((String) -> Boolean)? = null,
        filterErr: ((String) -> Boolean)? = null
    ): CompletableFuture<Pair<List<String>, List<String>>> =
        hostsFile(hosts).thenCompose { hostsFile ->
            CompletableFuture.supplyAsync {
                Runtime.getRuntime().exec(
                    args(binds(hostsFile)).plus(args).toTypedArray(),
                    vars().plus(vars).map { (key, value) -> "$key=$value" }.toTypedArray()
                )
            }.thenCompose { process ->
                fun InputStream.linesAsync(filter: ((String) -> Boolean)?) =
                    CompletableFuture.supplyAsync {
                        this.bufferedReader().lineSequence()
                            .filter { if (filter == null) true else filter(it) }.toList()
                    }

                val outFuture = process.inputStream.linesAsync(filterOut)
                val errFuture = process.errorStream.linesAsync(filterErr)

                outFuture.thenCompose { out ->
                    errFuture.thenApplyAsync { err ->
                        val exitCode = process.waitFor()
                        if (exitCode == 0) Pair(out, err)
                        else throw ResticException(exitCode, err)
                    }
                }
            }.handle { result, exception ->
                hostsFile.delete()

                if (exception != null) throw exception
                result
            }
        }

    fun version(): CompletableFuture<String> =
        restic(listOf("version")).thenApply { (out, _) -> out[0] }
}

object ResticSnapshotIdSerializer : KSerializer<ResticSnapshotId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SnapshotId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ResticSnapshotId) =
        encoder.encodeString(value.id)

    override fun deserialize(decoder: Decoder): ResticSnapshotId =
        ResticSnapshotId(decoder.decodeString())
}

@Serializable(with = ResticSnapshotIdSerializer::class)
data class ResticSnapshotId(val id: String) {
    val short get() = id.take(8)
}

object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ZonedDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ZonedDateTime) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): ZonedDateTime =
        ZonedDateTime.parse(decoder.decodeString())
}

object FileSerializer : KSerializer<File> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ZonedDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: File) = encoder.encodeString(value.path)
    override fun deserialize(decoder: Decoder): File = File(decoder.decodeString())
}

@Serializable
data class ResticSnapshot(
    val time: @Serializable(with = ZonedDateTimeSerializer::class) ZonedDateTime,
    val parent: ResticSnapshotId? = null,
    val tree: String,
    val paths: List<@Serializable(with = FileSerializer::class) File>,
    val hostname: String,
    val id: String//SnapshotId
)

@Serializable
data class ResticFile(
    val name: String,
    val type: String,
    val path: @Serializable(with = FileSerializer::class) File,
    val mtime: @Serializable(with = ZonedDateTimeSerializer::class) ZonedDateTime,
    val atime: @Serializable(with = ZonedDateTimeSerializer::class) ZonedDateTime,
    val ctime: @Serializable(with = ZonedDateTimeSerializer::class) ZonedDateTime
)

@Serializable
data class ResticBackupProgress(
    val seconds_elapsed: Int,
    val percent_done: Int,
    val total_files: Long? = null,
    val files_done: Long,
    val total_bytes: Long? = null,
    val bytes_done: Long = 0
)

@Serializable
data class ResticBackupSummary(
    val files_new: Long,
    val files_changed: Long,
    val files_unmodified: Long,
    val dirs_new: Long,
    val dirs_changed: Long,
    val dirs_unmodified: Long,
    val data_blobs: Long,
    val tree_blobs: Long,
    val data_added: Long,
    val total_files_processed: Long,
    val total_bytes_processed: Long,
    val total_duration: Double,
    val snapshot_id: String
)

abstract class ResticRepo(
    val restic: Restic,
    private val password: String
) {
    protected abstract fun repository(): String

    protected open fun hosts(): List<String> = emptyList()

    protected open fun args(): List<String> = emptyList()

    protected open fun vars(): List<Pair<String, String>> = emptyList()

    private fun restic(
        args: List<String>,
        vars: List<Pair<String, String>> = emptyList(),
        filterOut: ((String) -> Boolean)? = null,
        filterErr: ((String) -> Boolean)? = null
    ) = restic.restic(
        args().plus(args),
        listOf(
            Pair("RESTIC_REPOSITORY", repository()),
            Pair("RESTIC_PASSWORD", password)
        ).plus(vars()).plus(vars),
        hosts()
    )

    private val format = Json { ignoreUnknownKeys = true }
    private val filterJson = { line: String -> line.startsWith("{") || line.startsWith("[") }

    fun init(): CompletableFuture<String> =
        restic(listOf("init")).thenApply { (out, _) ->
            out.joinToString("\n")
        }

    fun snapshots(): CompletableFuture<List<ResticSnapshot>> =
        restic(
            listOf("--json", "snapshots"),
            filterOut = filterJson
        ).thenApply { (out, _) ->
            val json = out[0]
            format.decodeFromString<List<ResticSnapshot>>(json)
        }

    fun forget(snapshotIds: List<ResticSnapshotId>): CompletableFuture<String> =
        restic(listOf("forget").plus(snapshotIds.map { it.id })).thenApply { (out, _) ->
            out.joinToString("\n")
        }

    fun ls(snapshotId: ResticSnapshotId): CompletableFuture<Pair<ResticSnapshot, List<ResticFile>>> =
        restic(
            listOf("--json", "ls", snapshotId.id),
            filterOut = filterJson
        ).thenApply { (out, _) ->
            val snapshotJson = out[0]
            val filesJson = out.drop(1)
            Pair(
                format.decodeFromString<ResticSnapshot>(snapshotJson),
                filesJson.map { format.decodeFromString<ResticFile>(it) }
            )
        }

    fun backup(
        path: File,
        onProgress: (ResticBackupProgress) -> Void
    ): CompletableFuture<ResticBackupSummary> =
        restic(
            listOf("--json", "backup", path.absolutePath),
            filterOut = { line ->
                val isJson = filterJson(line)
                if (isJson && line.contains("\"message_type\":\"status\"")) {
                    val progress = format.decodeFromString<ResticBackupProgress>(line)
                    onProgress(progress)
                    false
                } else
                    isJson
            }
        ).thenApply { (out, _) ->
            val json = out[0]
            format.decodeFromString<ResticBackupSummary>(json)
        }
}

class ResticRepoS3(
    restic: Restic,
    password: String,
    private val s3Url: URI,
    private val accessKeyId: String,
    private val secretAccessKey: String
) : ResticRepo(
    restic,
    password
) {

    override fun repository(): String = "s3:$s3Url"

    override fun hosts(): List<String> = listOf(s3Url.host)

    override fun vars(): List<Pair<String, String>> = listOf(
        Pair("AWS_ACCESS_KEY_ID", accessKeyId),
        Pair("AWS_SECRET_ACCESS_KEY", secretAccessKey)
    )
}
