package de.lolhens.resticui.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

data class Config(
    val repos: List<RepoConfig>,
    val folders: List<Pair<FolderConfig, RepoConfig>>
) {
    companion object {
        val format = Json { ignoreUnknownKeys = true }

        fun fromJsonString(json: String): Config = format.decodeFromString(ConfigSerializer, json)
    }

    fun toJsonString(): String = format.encodeToString(ConfigSerializer, this)
}

object ConfigSerializer : KSerializer<Config> {
    @Serializable
    private data class ConfigSurrogate(
        val repos: List<@Serializable(with = RepoConfigSerializer::class) RepoConfig>,
        val folders: List<Pair<
                FolderConfig,
                @Serializable(with = RepoConfigIdSerializer::class) RepoConfigId
                >>
    )

    override val descriptor: SerialDescriptor = ConfigSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Config) {
        val surrogate = ConfigSurrogate(
            value.repos,
            value.folders.map { Pair(it.first, it.second.base.id) }
        )
        encoder.encodeSerializableValue(ConfigSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Config {
        val surrogate = decoder.decodeSerializableValue(ConfigSurrogate.serializer())
        return Config(
            surrogate.repos,
            surrogate.folders.flatMap { directory ->
                val repo = surrogate.repos.find { repo -> repo.base.id == directory.second }
                if (repo == null)
                    emptySequence()
                else
                    sequenceOf(Pair(directory.first, repo))
            }
        )
    }
}