package de.lolhens.resticui

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.security.crypto.MasterKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URI
import java.util.*

class ConfigManager(context: Context) {
    companion object {
        fun checkPermission(activity: Activity, permission: String, requestCode: Int): Boolean {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    permission
                ) == PackageManager.PERMISSION_DENIED
            ) {
                // Requesting the permission
                ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
                return false
            } else {
                return true
            }
        }
    }

    private val mainKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
}

data class Config(
    val repos: List<RepoParams>,
    val directories: List<Pair<File, RepoConfigId>>
)

object RepoConfigIdSerializer : KSerializer<RepoConfigId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SnapshotId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: RepoConfigId) =
        encoder.encodeString(value.id.toString())

    override fun deserialize(decoder: Decoder): RepoConfigId =
        RepoConfigId(UUID.fromString(decoder.decodeString()))
}

data class RepoConfigId(val id: UUID)

enum class RepoType(type: String) {
    S3("s3") {
        override fun repoParams(json: String): RepoParams =
            format.decodeFromString<S3RepoParams>(json)

    };

    companion object {
        private val format = Json { ignoreUnknownKeys = true }

        fun decodeFromJsonString(json: String): RepoConfig {
            val config = format.decodeFromString<RepoConfig>(json)
            return config.copy(params = config.type.repoParams(json))
        }

    }

    abstract fun repoParams(json: String): RepoParams
}

@Serializable
data class RepoConfig(
    val id: @Serializable(with = RepoConfigIdSerializer::class) RepoConfigId,
    val type: RepoType,
    val password: String,
    val params: RepoParams? = null // TODO String?
)

@Serializable
abstract class RepoParams {
    abstract fun repo(repoConfig: RepoConfig, restic: Restic): ResticRepo
}

object URISerializer : KSerializer<URI> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("URI", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: URI) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): URI = URI(decoder.decodeString())
}

@Serializable
data class S3RepoParams(
    val s3Url: @Serializable(with = URISerializer::class) URI,
    val accessKeyId: String,
    val secretAccessKey: String
) : RepoParams() {
    override fun repo(repoConfig: RepoConfig, restic: Restic): ResticRepo = ResticRepoS3(
        restic,
        repoConfig.password,
        s3Url,
        accessKeyId,
        secretAccessKey
    )
}
