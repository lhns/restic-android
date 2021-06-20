package de.lolhens.resticui.restic

import kotlinx.serialization.Serializable

@Serializable
data class ResticBackupProgress(
    val seconds_elapsed: Int,
    val percent_done: Int,
    val total_files: Long? = null,
    val files_done: Long,
    val total_bytes: Long? = null,
    val bytes_done: Long = 0
)