package de.lolhens.resticui.config

import de.lolhens.resticui.DurationSerializer
import de.lolhens.resticui.FileSerializer
import de.lolhens.resticui.ZonedDateTimeSerializer
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
    val lastBackup: @Serializable(with = ZonedDateTimeSerializer::class) ZonedDateTime?
) {
    fun repo(config: Config): RepoConfig? = config.repos.find { it.base.id == repoId }
}