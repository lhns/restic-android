package de.lolhens.resticui.restic

import android.bluetooth.BluetoothAdapter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Duration
import java.util.concurrent.CompletableFuture

abstract class ResticRepo(
    val restic: Restic,
    private val password: String
) {
    companion object {
        private val format = Json { ignoreUnknownKeys = true }

        private const val DEFAULT_HOSTNAME = "android-device"

        private val filterJson = { line: String -> line.startsWith("{") || line.startsWith("[") }

        val hostname: String by lazy {
            val blueToothAdapter = BluetoothAdapter.getDefaultAdapter()

            if (blueToothAdapter != null) {
                // Some Devices do not have a BluetoothAdapter e.g. the Android Emulator. For this case we use a default
                // value
                BluetoothAdapter.getDefaultAdapter().name
            } else {
                return@lazy DEFAULT_HOSTNAME
            }
        }
    }

    abstract fun repository(): String

    protected open fun hosts(): List<String> = emptyList()

    protected open fun args(): List<String> = emptyList()

    protected open fun vars(): List<Pair<String, String>> = emptyList()

    private fun restic(
        args: List<String>,
        vars: List<Pair<String, String>> = emptyList(),
        filterOut: ((String) -> Boolean)? = null,
        filterErr: ((String) -> Boolean)? = null,
        cancel: CompletableFuture<Unit>? = null
    ) = restic.restic(
        args().plus(args),
        listOf(
            Pair("RESTIC_REPOSITORY", repository()),
            Pair("RESTIC_PASSWORD", password)
        ).plus(vars()).plus(vars),
        hosts(),
        filterOut,
        filterErr,
        cancel
    )

    fun init(): CompletableFuture<String> =
        restic(listOf("init")).thenApply { (out, _) ->
            out.joinToString("\n")
        }

    fun stats(): CompletableFuture<ResticStats> =
        restic(
            listOf("--json", "stats")
        ).thenApply { (out, _) ->
            val json = out.joinToString("\n")
            format.decodeFromString<ResticStats>(json)
        }

    fun snapshots(hostname: String? = null): CompletableFuture<List<ResticSnapshot>> =
        restic(
            listOf("--json", "snapshots").plus(
                if (hostname != null) listOf("--host", hostname)
                else emptyList()
            )
        ).thenApply { (out, _) ->
            val json = out.joinToString("\n")
            format.decodeFromString<List<ResticSnapshot>>(json)
        }

    fun cat(snapshotId: ResticSnapshotId): CompletableFuture<ResticSnapshot> =
        restic(
            listOf("--json", "cat", "snapshot", snapshotId.id)
        ).thenApply { (out, _) ->
            val json = "{\"id\": \"${snapshotId.id}\",${out.joinToString("\n").drop(1)}"
            format.decodeFromString<ResticSnapshot>(json)
        }

    fun forget(snapshotIds: List<ResticSnapshotId>, prune: Boolean): CompletableFuture<String> =
        restic(
            listOf("forget").plus(
                if (prune) listOf("--prune")
                else emptyList()
            ).plus(snapshotIds.map { it.id })
        ).thenApply { (out, _) ->
            out.joinToString("\n")
        }

    fun forget(
        keepLast: Int?,
        keepWithin: Duration?,
        prune: Boolean
    ): CompletableFuture<List<ResticSnapshot>> =
        restic(
            listOf("forget").plus(
                if (prune) listOf("--prune")
                else emptyList()
            ).plus(
                if (keepLast != null) listOf("--keep-last", keepLast.toString())
                else emptyList()
            ).plus(
                if (keepWithin != null) {
                    val hours = keepWithin.toHours()
                    listOf("--keep-within", "${hours / 24}d${hours % 24}h")
                } else emptyList()
            )
        ).thenApply { (out, _) ->
            val json = out.joinToString("\n")
            format.decodeFromString<List<ResticForgetResult>>(json).flatMap { it.remove }
        }

    fun unlock(): CompletableFuture<String> =
        restic(listOf("unlock")).thenApply { (out, _) ->
            out.joinToString("\n")
        }

    fun ls(snapshotId: ResticSnapshotId): CompletableFuture<Pair<ResticSnapshot, List<ResticFile>>> =
        restic(
            listOf("--json", "ls", snapshotId.id),
        ).thenApply { (out, _) ->
            val snapshotJson = out[0]
            val filesJson = out.drop(1)
            Pair(
                format.decodeFromString<ResticSnapshot>(snapshotJson),
                filesJson.map { format.decodeFromString<ResticFile>(it) }
            )
        }

    fun backup(
        hostname: String,
        path: File,
        onProgress: (ResticBackupProgress) -> Unit,
        cancel: CompletableFuture<Unit>? = null
    ): CompletableFuture<ResticBackupSummary> =
        restic(
            listOf("--json", "backup", "--host", hostname, path.absolutePath),
            filterOut = { line ->
                val isStatus = line.contains("\"message_type\":\"status\"")
                if (isStatus) {
                    val progress = format.decodeFromString<ResticBackupProgress>(line)
                    onProgress(progress)
                }
                !isStatus
            },
            cancel = cancel
        ).thenApply { (out, _) ->
            val json = out.joinToString("\n")
            format.decodeFromString<ResticBackupSummary>(json)
        }
}