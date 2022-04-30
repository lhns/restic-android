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
    companion object {
        fun zero() = ResticBackupProgress(percent_done = 0.0)
    }

    fun percentDone100() = percent_done * 100

    fun percentDoneString() =
        (when {
            percent_done == 0.0 -> "0"
            percentDone100() < 0.01 -> "%.4f".format(percentDone100())
            percentDone100() < 1 -> "%.2f".format(percentDone100())
            else -> percentDone100().roundToInt().toString()
        }) + "%"

    private fun formatBytes(bytes: Long?) =
        when {
            bytes == null -> null
            bytes >= 1_000_000_000 -> "${"%.3f".format(bytes / 10_000_000 / 100.0)}GB"
            bytes >= 1_000_000 -> "${"%.2f".format(bytes / 10_000 / 100.0)}MB"
            bytes >= 1_000 -> "${"%d".format(bytes / 10 / 100)}KB"
            else -> "$bytes B"
        }

    fun totalBytesString() = formatBytes(total_bytes)
    fun bytesDoneString() = formatBytes(bytes_done)

    fun timeElapsedString() =
        "${seconds_elapsed / 60}:${"0${seconds_elapsed % 60}".takeLast(2)}"
}