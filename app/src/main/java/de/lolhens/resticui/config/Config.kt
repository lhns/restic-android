package de.lolhens.resticui.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Config(
    val repos: List<RepoConfig>,
    val folders: List<FolderConfig>,
    val hostname: String?,
    val nameServers: List<String>?
) {
    companion object {
        val format = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

        fun fromJsonString(json: String): Config = format.decodeFromString(serializer(), json)
    }

    fun toJsonString(): String = format.encodeToString(serializer(), this)
}