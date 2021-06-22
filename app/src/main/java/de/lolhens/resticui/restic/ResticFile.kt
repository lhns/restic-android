package de.lolhens.resticui.restic

import de.lolhens.resticui.FileSerializer
import de.lolhens.resticui.ZonedDateTimeSerializer
import kotlinx.serialization.Serializable
import java.io.File
import java.time.ZonedDateTime

@Serializable
data class ResticFile(
    val name: String,
    val type: String,
    val path: @Serializable(with = FileSerializer::class) File,
    val mtime: @Serializable(with = ZonedDateTimeSerializer::class) ZonedDateTime,
    val atime: @Serializable(with = ZonedDateTimeSerializer::class) ZonedDateTime,
    val ctime: @Serializable(with = ZonedDateTimeSerializer::class) ZonedDateTime
)