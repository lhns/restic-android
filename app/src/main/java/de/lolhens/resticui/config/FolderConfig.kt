package de.lolhens.resticui.config

import de.lolhens.resticui.DurationSerializer
import de.lolhens.resticui.FileSerializer
import de.lolhens.resticui.ui.folder.FolderEditFragment
import kotlinx.serialization.Serializable
import java.io.File
import java.time.Duration
import java.time.ZonedDateTime

@Serializable
data class FolderConfig(
    val id: @Serializable(with = FolderConfigIdSerializer::class) FolderConfigId,
    val repoId: @Serializable(with = RepoConfigIdSerializer::class) RepoConfigId,
    val path: @Serializable(with = FileSerializer::class) File,
    val schedule: String,
    val keepLast: Int? = null,
    val keepWithin: @Serializable(with = DurationSerializer::class) Duration? = null,
    val history: List<BackupHistoryEntry> = emptyList()
) {
    fun repo(config: Config): RepoConfig? = config.repos.find { it.base.id == repoId }

    fun plusHistoryEntry(entry: BackupHistoryEntry): FolderConfig {
        var newHistory = history.take(20)
        newHistory =
            if (newHistory.any { it.scheduled }) newHistory
            else newHistory.plus(history.filter { it.scheduled }.take(1))
        newHistory =
            if (newHistory.any { it.successful }) newHistory
            else newHistory.plus(history.filter { it.scheduled }.take(1))
        return copy(history = newHistory.plus(entry).sortedBy { it.timestamp })
    }

    fun lastBackup(
        filterScheduled: Boolean = false,
        filterSuccessful: Boolean = false
    ): BackupHistoryEntry? =
        history.find { (!filterScheduled || it.scheduled) && (!filterSuccessful || it.successful) }

    fun shouldBackup(dateTime: ZonedDateTime): Boolean {
        val scheduleMinutes = FolderEditFragment.schedules.find { it.first == schedule }?.second
        if (scheduleMinutes == null || scheduleMinutes < 0) return false
        val lastBackup = lastBackup(filterScheduled = true)?.timestamp
        if (lastBackup == null) return true
        var quantized = lastBackup.withMinute(0).withSecond(0).withNano(0)
        if (scheduleMinutes >= 24 * 60)
            quantized = quantized.withHour(0)
        val nextBackup = quantized.plusMinutes(scheduleMinutes.toLong())
        return !dateTime.isBefore(nextBackup)
    }
}