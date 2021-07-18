package de.lolhens.resticui.config

import de.lolhens.resticui.DurationSerializer
import de.lolhens.resticui.ZonedDateTimeSerializer
import de.lolhens.resticui.restic.ResticSnapshotId
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.ZonedDateTime

@Serializable
data class BackupHistoryEntry(
    val timestamp: @Serializable(with = ZonedDateTimeSerializer::class) ZonedDateTime,
    val duration: @Serializable(with = DurationSerializer::class) Duration,
    val scheduled: Boolean,
    val cancelled: Boolean,
    val snapshotId: ResticSnapshotId?,
    val errorMessage: String?
) {
    val successful get() = snapshotId != null
}