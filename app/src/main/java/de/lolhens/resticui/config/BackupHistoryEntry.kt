package de.lolhens.resticui.config

import de.lolhens.resticui.HourDuration
import de.lolhens.resticui.HourDurationSerializer
import de.lolhens.resticui.ZonedDateTimeSerializer
import de.lolhens.resticui.restic.ResticSnapshotId
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Serializable
data class BackupHistoryEntry(
    val timestamp: @Serializable(with = ZonedDateTimeSerializer::class) ZonedDateTime,
    val duration: @Serializable(with = HourDurationSerializer::class) HourDuration,
    val scheduled: Boolean,
    val cancelled: Boolean,
    val snapshotId: ResticSnapshotId?,
    val errorMessage: String?
) {
    val successful get() = snapshotId != null
}