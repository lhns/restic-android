package de.lolhens.resticui

import android.content.Context
import java.io.File
import java.io.InputStream
import java.net.InetAddress
import java.net.URL
import java.nio.charset.StandardCharsets
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

class ResticException(exitCode: Int, stderr: Array<String>) :
    Exception("Restic error $exitCode:\n${stderr.joinToString("\n")}")

abstract class Restic(
    private val storage: ResticStorage
) {
    private val proot = storage.lib().resolve("proot")
    private val restic = storage.lib().resolve("restic")
    private val loader = storage.lib().resolve("loader")
    private val loader32 = storage.lib().resolve("loader32")

    private fun binds(hostsFile: File): Array<Pair<String, String>> = arrayOf(
        Pair(hostsFile.absolutePath, "/etc/hosts"),
        Pair("/system", "/system"),
        Pair("/storage", "/storage"),
        Pair("/data", "/data"),
        Pair(storage.cache().absolutePath, "/cache")
    )

    private fun args(binds: Array<Pair<String, String>>): Array<String> =
        arrayOf(
            proot.absolutePath,
        ).plus(
            binds.flatMap { (from, to) -> listOf("-b", "$from:$to") }
        ).plus(
            arrayOf(
                "--kill-on-exit",
                restic.absolutePath
            )
        )

    private fun vars(): Array<Pair<String, String>> = arrayOf(
        Pair("PATH", "/system/bin"),
        Pair("LD_LIBRARY_PATH", storage.lib().absolutePath),
        Pair("PROOT_LOADER", loader.absolutePath),
        Pair("PROOT_LOADER_32", loader32.absolutePath),
        Pair("PROOT_TMP_DIR", storage.cache().absolutePath),
        Pair("RESTIC_CACHE_DIR", "/cache/restic")
    )

    private fun hostsFile(hosts: Array<String>): CompletableFuture<File> =
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
        args: Array<String>,
        vars: Array<Pair<String, String>> = emptyArray(),
        hosts: Array<String> = emptyArray()
    ): CompletableFuture<Pair<Array<String>, Array<String>>> =
        hostsFile(hosts).thenCompose { hostsFile ->
            CompletableFuture.supplyAsync {
                Runtime.getRuntime().exec(
                    args(binds(hostsFile)).plus(args),
                    vars().plus(vars).map { (key, value) -> "$key=$value" }.toTypedArray()
                )
            }.thenCompose { process ->
                fun InputStream.linesAsync() = CompletableFuture.supplyAsync {
                    this.bufferedReader().lineSequence().toList().toTypedArray()
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
        restic(arrayOf("version")).thenApply { (out, _) -> out[0] }
}

abstract class ResticRepo(
    val restic: Restic,
    private val password: String
) {
    protected abstract fun repository(): String

    protected open fun hosts(): Array<String> = emptyArray()

    protected open fun args(): Array<String> = emptyArray()

    protected open fun vars(): Array<Pair<String, String>> = emptyArray()

    private fun restic(
        args: Array<String>,
        vars: Array<Pair<String, String>> = emptyArray()
    ) = restic.restic(
        args().plus(args),
        arrayOf(
            Pair("RESTIC_REPOSITORY", repository()),
            Pair("RESTIC_PASSWORD", password)
        ).plus(vars()).plus(vars),
        hosts()
    )

    fun init(): CompletableFuture<Void> =
        restic(arrayOf("--json", "init")).thenApply { (out, err) ->
            println(out)
            println(err)
            null
        }

    fun snapshots(): CompletableFuture<Array<String>> =
        restic(arrayOf("--json", "snapshots")).thenApply { (out, err) ->
            println(out)
            println(err)
            out.plus(err)
        }

    fun forget(snapshotId: String): CompletableFuture<Void> =
        restic(arrayOf("--json", "forget", snapshotId)).thenApply { (out, err) ->
            println(out)
            println(err)
            null
        }

    fun ls(snapshotId: String): CompletableFuture<Void> =
        restic(arrayOf("--json", "ls", snapshotId)).thenApply { (out, err) ->
            println(out)
            println(err)
            null
        }

    fun backup(path: File): CompletableFuture<String> =
        restic(arrayOf("--json", "backup", path.absolutePath)).thenApply { (out, err) ->
            println(out)
            println(err)
            null
        }
}

class ResticRepoS3(
    restic: Restic,
    password: String,
    private val s3Url: URL,
    private val accessKeyId: String,
    private val secretAccessKey: String
) : ResticRepo(
    restic,
    password
) {

    override fun repository(): String = "s3:$s3Url"

    override fun hosts(): Array<String> = arrayOf(s3Url.host)

    override fun vars(): Array<Pair<String, String>> = arrayOf(
        Pair("AWS_ACCESS_KEY_ID", accessKeyId),
        Pair("AWS_SECRET_ACCESS_KEY", secretAccessKey)
    )
}
