package de.lolhens.resticui.config

import de.lolhens.resticui.FileSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.File

data class Config(
    val repos: List<RepoConfig>,
    val directories: List<Pair<File, RepoConfig>>
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
        val directories: List<Pair<
                @Serializable(with = FileSerializer::class) File,
                @Serializable(with = RepoConfigIdSerializer::class) RepoConfigId
                >>
    )

    override val descriptor: SerialDescriptor = ConfigSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Config) {
        val surrogate = ConfigSurrogate(
            value.repos,
            value.directories.map { Pair(it.first, it.second.base.id) }
        )
        encoder.encodeSerializableValue(ConfigSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Config {
        val surrogate = decoder.decodeSerializableValue(ConfigSurrogate.serializer())
        return Config(
            surrogate.repos,
            surrogate.directories.flatMap { directory ->
                val repo = surrogate.repos.find { repo -> repo.base.id == directory.second }
                if (repo == null)
                    emptySequence()
                else
                    sequenceOf(Pair(directory.first, repo))
            }
        )
    }
}
