package de.lolhens.resticui

import de.lolhens.resticui.restic.ResticBackupProgress
import de.lolhens.resticui.restic.ResticBackupSummary
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

data class ActiveBackup(
    val inProgress: Boolean,
    val progress: ResticBackupProgress?,
    val summary: ResticBackupSummary?,
    val error: String?,
    val cancelFuture: CompletableFuture<Unit>,
    val notificationId: Int
) {
    companion object {
        private val nextNotificationId = AtomicInteger(0)

        fun create() = ActiveBackup(
            inProgress = true,
            progress = null,
            summary = null,
            error = null,
            cancelFuture = CompletableFuture(),
            notificationId = nextNotificationId.getAndIncrement()
        )
    }

    fun progress(progress: ResticBackupProgress) = copy(
        progress = progress
    )

    fun finish(
        summary: ResticBackupSummary?,
        error: String?
    ) = copy(
        inProgress = false,
        summary = summary,
        error = error
    )

    fun cancel() {
        cancelFuture.complete(null)
    }

    fun isStarting() = inProgress && progress == null
}