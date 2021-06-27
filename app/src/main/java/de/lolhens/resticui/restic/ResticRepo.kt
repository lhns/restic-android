package de.lolhens.resticui.restic

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.CompletableFuture

abstract class ResticRepo(
    val restic: Restic,
    private val password: String
) {
    companion object {
        private val format = Json { ignoreUnknownKeys = true }

        private val filterJson = { line: String -> line.startsWith("{") || line.startsWith("[") }
    }

    abstract fun repository(): String

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
        hosts(),
        filterOut,
        filterErr
    )

    fun init(): CompletableFuture<String> =
        restic(listOf("init")).thenApply { (out, _) ->
            out.joinToString("\n")
        }

    fun stats(): CompletableFuture<ResticStats> =
        restic(
            listOf("--json", "stats"),
            filterOut = filterJson
        ).thenApply { (out, _) ->
            val json = out[0]
            format.decodeFromString<ResticStats>(json)
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
        onProgress: (ResticBackupProgress) -> Unit
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