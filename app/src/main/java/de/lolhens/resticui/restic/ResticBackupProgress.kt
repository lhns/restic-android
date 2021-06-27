package de.lolhens.resticui.restic

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
data class ResticBackupProgress(
    val seconds_elapsed: Int = 0,
    val percent_done: Double,
    val total_files: Long? = null,
    val files_done: Long = 0,
    val total_bytes: Long? = null,
    val bytes_done: Long = 0
) {
    fun percentDoneString() =
        (if (percent_done < 0.01) "%.4f".format(percent_done)
        else if (percent_done < 1) "%.2f".format(percent_done)
        else percent_done.roundToInt().toString()) + "%"

    private fun formatBytes(bytes: Long?) =
        if (bytes == null) null
        else if (bytes >= 1_000_000_000) "${"%.2f".format(bytes / 10_000_000 / 100.0)} GB"
        else if (bytes >= 1_000_000) "${"%.2f".format(bytes / 10_000 / 100.0)} MB"
        else if (bytes >= 1_000) "${"%.2f".format(bytes / 10 / 100.0)} KB"
        else "$bytes B"

    fun totalBytesString() = formatBytes(total_bytes)
    fun bytesDoneString() = formatBytes(bytes_done)
}