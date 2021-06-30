package de.lolhens.resticui.config

import de.lolhens.resticui.FileSerializer
import de.lolhens.resticui.ZonedDateTimeSerializer
import kotlinx.serialization.Serializable
import java.io.File
import java.time.ZonedDateTime

@Serializable
data class FolderConfig(
    val id: @Serializable(with = FolderConfigIdSerializer::class) FolderConfigId,
    val repoId: @Serializable(with = RepoConfigIdSerializer::class) RepoConfigId,
    val path: @Serializable(with = FileSerializer::class) File,
    val schedule: String,
    val lastBackup: @Serializable(with = ZonedDateTimeSerializer::class) ZonedDateTime? = null
) {
    fun repo(config: Config): RepoConfig? = config.repos.find { it.base.id == repoId }
}