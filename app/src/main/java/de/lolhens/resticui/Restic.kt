package de.lolhens.resticui

import android.content.Context
import java.io.File
import java.io.InputStream
import java.net.InetAddress
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CompletableFuture

class ResticS3(
    context: Context,
    repositoryPassword: String,
    private val s3Url: URI,
    private val accessKeyId: String,
    private val secretAccessKey: String
) : Restic(
    context,
    repositoryPassword
) {
    override fun args(): CompletableFuture<Array<String>> =
        super.args().thenApply {
            it.plus(
                arrayOf(

                )
            )
        }

    override fun vars(): CompletableFuture<Array<Pair<String, String>>> =
        super.vars().thenApply {
            it.plus(
                arrayOf(
                    Pair("AWS_ACCESS_KEY_ID", accessKeyId),
                    Pair("AWS_SECRET_ACCESS_KEY", secretAccessKey)
                )
            )
        }

    override fun hosts(): Array<String> = arrayOf(s3Url.host)

    override fun repository(): String = "s3:$s3Url"
}

interface ResticStorage {
    companion object {
        fun fromContext(context: Context): ResticStorage = object: ResticStorage {
            private val _lib =       File(context.applicationInfo.nativeLibraryDir)
            override fun lib(): File = _lib
            override fun cache(): File = context.cacheDir
        }
    }

    fun lib(): File
    fun cache(): File
}

abstract class Restic(
    private val storage: ResticStorage
) {
    private val resticBin = storage.lib().resolve("restic")
    private val prootBin = storage.lib().resolve("proot")

    protected open fun args(): CompletableFuture<Array<String>> =
        CompletableFuture.supplyAsync {
            val hostsFileContent = hosts().map { host ->
                val address = InetAddress.getByName(host)
                "${address.hostAddress} $host"
            }.joinToString("\n")

            val hostsFile = File.createTempFile("hosts", "", storage.cache())
            hostsFile.writeText(hostsFileContent, StandardCharsets.UTF_8)
            hostsFile.deleteOnExit() // TODO

            arrayOf(
                prootBin.absolutePath,
                "-b", hostsFile.absolutePath + ":/etc/hosts",
                "-b", "/system:/system",
                "-b", "/storage:/storage",
                "-b", "/data:/data",
                "-b", storage.cache().absolutePath + ":/cache",
                "--kill-on-exit",
                resticBin.absolutePath,
                "--json",
            )
        }

    protected open fun vars(): CompletableFuture<Array<Pair<String, String>>> =
        CompletableFuture.completedFuture(
            arrayOf(
                Pair("PATH", "/system/bin"),
                Pair("LD_LIBRARY_PATH", storage.lib().absolutePath),
                Pair("PROOT_LOADER", storage.lib().resolve("loader").absolutePath),
                Pair("PROOT_LOADER_32", storage.lib().resolve("loader32").absolutePath),
                Pair("PROOT_TMP_DIR", storage.cache().absolutePath),
                Pair("RESTIC_CACHE_DIR", "/cache/restic")
            )
        )

    protected abstract fun hosts(): Array<String>

    protected abstract fun repository(): String

    fun snapshots(): CompletableFuture<Array<String>> {
        return restic(arrayOf("snapshots", "-r", repository())).thenApply { (err, out) ->
            err.plus(out)
        }
    }

    protected fun restic(args: Array<String>, vars: Array<Pair<String, String>>, repositoryPassword: String? = null): CompletableFuture<Pair<Array<String>, Array<String>>> =
        args().thenCompose { args0 ->
            vars().thenCompose { vars0 ->
                val passwordVar = Optional.ofNullable(repositoryPassword).map { Pair("RESTIC_PASSWORD", it) }

                val process = Runtime.getRuntime().exec(
                    args0.plus(args),
                    vars0/*.plus(passwordVar)*/.plus(vars).map { (key, value) -> "$key=$value" }.toTypedArray()
                )

                fun InputStream.linesAsync() = CompletableFuture.supplyAsync {
                    this.bufferedReader().lineSequence().toList().toTypedArray()
                }

                val outFuture = process.inputStream.linesAsync()
                val errFuture = process.errorStream.linesAsync()

                outFuture.thenCompose { out ->
                    errFuture.thenApply { err ->
                        Pair(err, out)
                    }
                }
            }
        }
}