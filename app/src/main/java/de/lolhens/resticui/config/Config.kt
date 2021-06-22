package de.lolhens.resticui.config

import de.lolhens.resticui.FileSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Config(
    val repos: List<@Serializable(with = RepoConfigSerializer::class) RepoConfig>,
    val directories: List<Pair<@Serializable(with = FileSerializer::class) File, @Serializable(with = RepoConfigIdSerializer::class) RepoConfigId>>
) {
    companion object {
        val format = Json { ignoreUnknownKeys = true }

        fun fromJsonString(json: String): Config = format.decodeFromString(json)
    }

    fun toJsonString(): String = format.encodeToString(this)
}
