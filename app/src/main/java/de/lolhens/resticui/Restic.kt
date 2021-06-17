package de.lolhens.resticui

import android.content.Context
import kotlinx.serialization.Contextual
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
        }
    }

    fun lib(): File
    fun cache(): File
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
        Pair(hostsFile.absolutePath, "/etc/hosts"),
        Pair("/system", "/system"),
        Pair("/storage", "/storage"),
        Pair("/data", "/data"),
        Pair(storage.cache().absolutePath, "/cache")
    )

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
        Pair("RESTIC_CACHE_DIR", "/cache/restic")
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
        hosts: List<String> = emptyList()
    ): CompletableFuture<Pair<List<String>, List<String>>> =
        hostsFile(hosts).thenCompose { hostsFile ->
            CompletableFuture.supplyAsync {
                Runtime.getRuntime().exec(
                    args(binds(hostsFile)).plus(args).toTypedArray(),
                    vars().plus(vars).map { (key, value) -> "$key=$value" }.toTypedArray()
                )
            }.thenCompose { process ->
                fun InputStream.linesAsync() = CompletableFuture.supplyAsync {
                    this.bufferedReader().lineSequence().toList()
                }

                val outFuture = process.inputStream.linesAsync()
                val errFuture = process.errorStream.linesAsync()

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

object SnapshotIdSerializer : KSerializer<SnapshotId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SnapshotId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SnapshotId) =
        encoder.encodeString(value.id)

    override fun deserialize(decoder: Decoder): SnapshotId =
        SnapshotId(decoder.decodeString())
}

@Serializable(with = SnapshotIdSerializer::class)
data class SnapshotId(val id: String) {
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
data class Snapshot(
    val time: @Serializable(with = ZonedDateTimeSerializer::class) ZonedDateTime,
    val parent: SnapshotId? = null,
    val tree: String,
    val paths: List<@Serializable(with = FileSerializer::class) File>,
    val hostname: String,
    val id: String//SnapshotId
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
        vars: List<Pair<String, String>> = emptyList()
    ) = restic.restic(
        args().plus(args),
        listOf(
            Pair("RESTIC_REPOSITORY", repository()),
            Pair("RESTIC_PASSWORD", password)
        ).plus(vars()).plus(vars),
        hosts()
    )

    private val format = Json { ignoreUnknownKeys = true }

    fun init(): CompletableFuture<Void> =
        restic(listOf("--json", "init")).thenApply { (out, err) ->
            println(out.joinToString("\n"))
            println(err.joinToString("\n"))
            null
        }

    fun snapshots(): CompletableFuture<List<Snapshot>> =
        restic(listOf("--json", "snapshots")).thenApply { (out, err) ->
            val json = out[0]
            format.decodeFromString<List<Snapshot>>(json)
        }

    fun forget(snapshotId: SnapshotId): CompletableFuture<Void> =
        restic(listOf("--json", "forget", snapshotId.id)).thenApply { (out, err) ->
            println(out.joinToString("\n"))
            println(err.joinToString("\n"))
            null
        }

    fun ls(snapshotId: SnapshotId): CompletableFuture<Void> =
        restic(listOf("--json", "ls", snapshotId.id)).thenApply { (out, err) ->
            println(out.joinToString("\n"))
            println(err.joinToString("\n"))
            null
        }

    fun backup(path: File): CompletableFuture<SnapshotId> =
        restic(listOf("--json", "backup", path.absolutePath)).thenApply { (out, err) ->
            println(out.joinToString("\n"))
            println(err.joinToString("\n"))
            null
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
