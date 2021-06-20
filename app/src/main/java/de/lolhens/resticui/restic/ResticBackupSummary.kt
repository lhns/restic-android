package de.lolhens.resticui.restic

import kotlinx.serialization.Serializable

@Serializable
data class ResticBackupSummary(
    val files_new: Long,
    val files_changed: Long,
    val files_unmodified: Long,
    val dirs_new: Long,
    val dirs_changed: Long,
    val dirs_unmodified: Long,
    val data_blobs: Long,
    val tree_blobs: Long,
    val data_added: Long,
    val total_files_processed: Long,
    val total_bytes_processed: Long,
    val total_duration: Double,
    val snapshot_id: String
)