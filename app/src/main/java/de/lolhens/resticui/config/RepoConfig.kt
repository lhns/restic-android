package de.lolhens.resticui.config

import de.lolhens.resticui.URISerializer
import de.lolhens.resticui.restic.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import java.net.URI
import java.util.*

@Serializable(with = RepoConfigSerializer::class)
data class RepoConfig(
    val base: RepoBaseConfig,
    val params: RepoParams
) {
    fun repo(restic: Restic): ResticRepo = params.repo(base, restic)
}

object RepoConfigSerializer : KSerializer<RepoConfig> {
    @Serializable
    private data class RepoConfigSurrogate(
        val base: RepoBaseConfig,
        val params: JsonObject
    )

    override val descriptor: SerialDescriptor = RepoConfigSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: RepoConfig) {
        val surrogate = RepoConfigSurrogate(
            value.base,
            value.base.type.serializeParams(value.params)
        )
        encoder.encodeSerializableValue(RepoConfigSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): RepoConfig {
        val surrogate = decoder.decodeSerializableValue(RepoConfigSurrogate.serializer())
        return RepoConfig(
            surrogate.base,
            surrogate.base.type.deserializeParams(surrogate.params)
        )
    }
}

@Serializable
data class RepoBaseConfig(
    val id: RepoConfigId,
    val name: String,
    val type: RepoType,
    val password: Secret
) {
    companion object {
        fun create(
            name: String,
            type: RepoType,
            password: Secret
        ): RepoBaseConfig =
            RepoBaseConfig(
                RepoConfigId(UUID.randomUUID()),
                name,
                type,
                password
            )
    }
}

enum class RepoType(type: String) {
    S3("s3") {
        override fun serializeParams(value: RepoParams): JsonObject =
            Config.format.decodeFromString(Config.format.encodeToString(value as S3RepoParams))

        override fun deserializeParams(json: JsonObject): S3RepoParams =
            Config.format.decodeFromString(Config.format.encodeToString(json))

    },
    Rest("rest") {
        override fun serializeParams(value: RepoParams): JsonObject =
            Config.format.decodeFromString(Config.format.encodeToString(value as RestRepoParams))

        override fun deserializeParams(json: JsonObject): RestRepoParams =
            Config.format.decodeFromString(Config.format.encodeToString(json))

    },
    B2("b2") {
        override fun serializeParams(value: RepoParams): JsonObject =
            Config.format.decodeFromString(Config.format.encodeToString(value as B2RepoParams))

        override fun deserializeParams(json: JsonObject): B2RepoParams =
            Config.format.decodeFromString(Config.format.encodeToString(json))

    };

    abstract fun serializeParams(value: RepoParams): JsonObject

    abstract fun deserializeParams(json: JsonObject): RepoParams
}

abstract class RepoParams {
    internal abstract fun repo(baseConfig: RepoBaseConfig, restic: Restic): ResticRepo
}

@Serializable
data class S3RepoParams(
    val s3Url: @Serializable(with = URISerializer::class) URI,
    val accessKeyId: String,
    val secretAccessKey: Secret
) : RepoParams() {
    override fun repo(baseConfig: RepoBaseConfig, restic: Restic): ResticRepo = ResticRepoS3(
        restic,
        baseConfig.password.secret,
        s3Url,
        accessKeyId,
        secretAccessKey.secret
    )
}

@Serializable
data class RestRepoParams(
    val restUrl: @Serializable(with = URISerializer::class) URI,
) : RepoParams() {
    override fun repo(baseConfig: RepoBaseConfig, restic: Restic): ResticRepo = ResticRepoRest(
        restic,
        baseConfig.password.secret,
        restUrl
    )
}

@Serializable
data class B2RepoParams(
    val b2Url: @Serializable(with = URISerializer::class) URI,
    val b2AccountId: String,
    val b2AccountKey: Secret
) : RepoParams() {
    override fun repo(baseConfig: RepoBaseConfig, restic: Restic): ResticRepo = ResticRepoB2(
        restic,
        baseConfig.password.secret,
        b2Url,
        b2AccountId,
        b2AccountKey.secret
    )
}