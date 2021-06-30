package de.lolhens.resticui

import de.lolhens.resticui.restic.ResticBackupProgress
import de.lolhens.resticui.restic.ResticBackupSummary
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

data class ActiveBackup(
    val progress: ResticBackupProgress?,
    val summary: ResticBackupSummary?,
    val error: String?,
    val cancelFuture: CompletableFuture<Unit>,
    val notificationId: Int
) {
    companion object {
        private val nextNotificationId = AtomicInteger(0)

        fun create() = ActiveBackup(
            null,
            null,
            null,
            CompletableFuture(),
            nextNotificationId.getAndIncrement()
        )
    }

    fun isStarting() = isInProgress() && progress == null

    fun isInProgress() = summary == null && error == null

    fun cancel() {
        cancelFuture.complete(null)
    }
}