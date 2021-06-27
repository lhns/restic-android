package de.lolhens.resticui.config

import de.lolhens.resticui.FileSerializer
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class FolderConfig(
    val id: @Serializable(with = FolderConfigIdSerializer::class) FolderConfigId,
    val path: @Serializable(with = FileSerializer::class) File,
    val schedule: String
)