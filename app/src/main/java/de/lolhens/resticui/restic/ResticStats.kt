package de.lolhens.resticui.restic

import kotlinx.serialization.Serializable

@Serializable
data class ResticStats(
    val total_size: Long,
    val total_file_count: Long
)