package de.lolhens.resticui.config

import de.lolhens.resticui.FileSerializer
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class FolderConfig(
    val directory: @Serializable(with = FileSerializer::class) File
)