package de.lolhens.resticui

import de.lolhens.resticui.restic.ResticBackupProgress
import de.lolhens.resticui.restic.ResticBackupSummary
import java.util.concurrent.CompletableFuture

data class ActiveBackup(
    val progress: ResticBackupProgress?,
    val summary: ResticBackupSummary?,
    val error: String?,
    val cancelFuture: CompletableFuture<Unit>
) {
    fun isStarting() = isInProgress() && progress == null

    fun isInProgress() = summary == null && error == null

    fun cancel() {
        cancelFuture.complete(null)
    }
}