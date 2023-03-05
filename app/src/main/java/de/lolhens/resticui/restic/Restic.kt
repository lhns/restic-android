package de.lolhens.resticui.restic

import android.system.Os
import android.util.Base64
import java.io.File
import java.io.InputStream
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.X509Certificate
import java.util.concurrent.CompletableFuture

class Restic(
    val storage: ResticStorage,
    val hostname: String,
    val nameServers: ResticNameServers
) {
    fun withHostname(hostname: String): Restic =
        Restic(storage, hostname, nameServers)

    fun withNameServers(nameServers: ResticNameServers): Restic =
        Restic(storage, hostname, nameServers)

    private fun executable(name: String) =
        storage.lib().resolve("libdata_$name.so")

    private val lib = storage.cache().resolve("lib")

    private fun initLib(name: String) {
        lib.mkdirs()
        val linkFile = lib.resolve(name)
        linkFile.delete()
        Os.symlink(executable(name).absolutePath, linkFile.absolutePath)
    }

    init {
        initLib("libtalloc.so.2")
    }

    private val proot = executable("proot")
    private val restic = executable("restic")
    private val loader = executable("loader")
    private val loader32 = executable("loader32")

    private fun withProot(
        binds: List<Pair<String, String>>,
        command: List<String>
    ): List<String> =
        listOf(
            proot.absolutePath,
            "--kill-on-exit",
        ).plus(
            binds.flatMap { (from, to) -> listOf("-b", "$from:$to") }
        ).plus(
            command
        )

    private fun vars(): List<Pair<String, String>> = listOf(
        Pair("PATH", "/system/bin"),
        Pair("TMPDIR", storage.cache().absolutePath),
        Pair("LD_LIBRARY_PATH", lib.absolutePath),
        Pair("PROOT_LOADER", loader.absolutePath),
        Pair("PROOT_LOADER_32", loader32.absolutePath),
        Pair("PROOT_TMP_DIR", storage.cache().absolutePath),
        Pair("RESTIC_CACHE_DIR", storage.cache().resolve("restic").absolutePath)
    )

    private fun binds(): List<Pair<String, String>> = listOf(
        Pair("/system", "/system"),
        Pair("/data", "/data"),
    ).plus(storage.storage().map {
        val path = it.absolutePath
        Pair(path, path)
    })

    private fun <A> tempFileBind(
        mountedFile: Pair<String, ByteArray>,
        f: (Pair<String, String>) -> CompletableFuture<A>
    ): CompletableFuture<A> =
        CompletableFuture.supplyAsync {
            File.createTempFile("bind_", "", storage.cache())
        }.thenCompose { file ->
            CompletableFuture.supplyAsync {
                file.writeBytes(mountedFile.second)
            }.thenCompose {
                f(Pair(file.absolutePath, mountedFile.first))
            }.handle { result, exception ->
                file.delete()

                if (exception != null) throw exception
                result
            }
        }

    private fun nameServersFile(nameServers: List<String>): Pair<String, ByteArray> =
        Pair(
            "/etc/resolv.conf",
            nameServers.map { nameServer ->
                "nameserver $nameServer "
            }.joinToString("\n")
                .toByteArray(StandardCharsets.UTF_8)
        )

    private fun hostsFile(hosts: List<String>): Pair<String, ByteArray> =
        Pair(
            "/etc/hosts",
            hosts.map { host ->
                val address = InetAddress.getByName(host)
                "${address.hostAddress} $host"
            }.joinToString("\n")
                .toByteArray(StandardCharsets.UTF_8)
        )

    private fun certificatesFile(): Pair<String, ByteArray> {
        val keyStore = KeyStore.getInstance("AndroidCAStore")

        val certificates: List<X509Certificate> =
            if (keyStore != null) try {
                keyStore.load(null, null)
                keyStore.aliases().toList().flatMap { alias ->
                    val certificate = keyStore.getCertificate(alias)
                    if (certificate is X509Certificate)
                        listOf(certificate)
                    else
                        emptyList()
                }
            } catch (e: KeyStoreException) {
                e.printStackTrace()
                emptyList()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
                emptyList()
            } else {
                emptyList()
            }

        val encodedCertificates: String = certificates.flatMap { certificate ->
            listOf(
                "-----BEGIN CERTIFICATE-----"
            ).plus(
                Base64.encodeToString(certificate.encoded, Base64.DEFAULT).chunked(64)
            ).plusElement(
                "-----END CERTIFICATE-----"
            )
        }.joinToString("\n")

        return Pair(
            "/etc/ssl/certs/android.crt",
            encodedCertificates.toByteArray(StandardCharsets.UTF_8)
        )
    }

    fun restic(
        args: List<String>,
        vars: List<Pair<String, String>> = emptyList(),
        hosts: List<String> = emptyList(),
        filterOut: ((String) -> Boolean)? = null,
        filterErr: ((String) -> Boolean)? = null,
        cancel: CompletableFuture<Unit>? = null
    ): CompletableFuture<Pair<List<String>, List<String>>> =
        tempFileBind(nameServersFile(nameServers.nameServers())) { nameserversBind ->
            tempFileBind(hostsFile(hosts)) { hostsBind ->
                val certificatesFile = certificatesFile()
                tempFileBind(certificatesFile) { certificatesBind ->
                    CompletableFuture.supplyAsync {
                        Runtime.getRuntime().exec(
                            withProot(
                                listOf(
                                    nameserversBind,
                                    hostsBind,
                                    certificatesBind
                                ).plus(
                                    binds()
                                ),
                                listOf(restic.absolutePath).plus(
                                    args.plus(
                                        listOf(
                                            "--cacert", certificatesBind.second
                                        ).filterNot { certificatesFile.second.isEmpty() }
                                    )
                                )
                            ).toTypedArray(),
                            vars().plus(vars).map { (key, value) -> "$key=$value" }.toTypedArray()
                        )
                    }
                        .thenCompose { process ->
                            fun InputStream.linesAsync(filter: ((String) -> Boolean)?) =
                                CompletableFuture.supplyAsync {
                                    this.bufferedReader().lineSequence()
                                        .filter {
                                            if (filter == null) true
                                            else if (cancel == null || !cancel.isDone) filter(it)
                                            else false
                                        }.toList()
                                }

                            val outFuture = process.inputStream.linesAsync(filterOut)
                            val errFuture = process.errorStream.linesAsync(filterErr)

                            val future = outFuture.thenCompose { out ->
                                errFuture.thenApplyAsync { err ->
                                    val exitCode = process.waitFor()
                                    if (exitCode == 0) Pair(out, err)
                                    else throw ResticException(exitCode, err)
                                }
                            }

                            cancel?.thenRun {
                                future.completeExceptionally(
                                    ResticException(
                                        0,
                                        emptyList(),
                                        cancelled = true
                                    )
                                )
                                process.destroy()
                            }

                            future
                        }
                }
            }
        }

    fun version(): CompletableFuture<String> =
        restic(listOf("version")).thenApply { (out, _) -> out[0] }
}