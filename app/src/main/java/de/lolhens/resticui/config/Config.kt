package de.lolhens.resticui.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Config(
    val repos: List<@Serializable(with = RepoConfigSerializer::class) RepoConfig>,
    val folders: List<FolderConfig>
) {
    companion object {
        val format = Json { ignoreUnknownKeys = true }

        fun fromJsonString(json: String): Config = format.decodeFromString(serializer(), json)
    }

    fun toJsonString(): String = format.encodeToString(serializer(), this)
}