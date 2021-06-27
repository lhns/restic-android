package de.lolhens.resticui.config

import de.lolhens.resticui.FileSerializer
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class FolderConfig(
    val id: @Serializable(with = FolderConfigIdSerializer::class) FolderConfigId,
    val repoId: @Serializable(with = RepoConfigIdSerializer::class) RepoConfigId,
    val path: @Serializable(with = FileSerializer::class) File,
    val schedule: String
) {
    fun repo(config: Config): RepoConfig? = config.repos.find { it.base.id == repoId }
}