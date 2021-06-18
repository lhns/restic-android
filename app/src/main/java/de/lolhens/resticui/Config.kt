package de.lolhens.resticui

import android.content.Context
import androidx.security.crypto.MasterKey
import java.io.File

class ConfigManager(context: Context) {
    private val mainKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
}

data class Config(
    val repos: List<RepoConfig>,
    val directories: List<Pair<File, RepoConfig>>
)

enum class RepoConfig(type: String) {
    S3("s3") {
        override fun repo(): ResticRepo {
            TODO("Not yet implemented")
        }

    };

    abstract fun repo(): ResticRepo
}