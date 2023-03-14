package de.lolhens.resticui.config

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Instant

class ConfigManager(
    context: Context,
    configFileName: String = "config.json",
    encryptedConfigFileName: String = "config.json.enc"
) {
    private val configFile = context.dataDir.resolve(configFileName)
    private val configFileTmp = context.dataDir.resolve("$configFileName.tmp")
    private val encryptedConfigFile = context.dataDir.resolve(encryptedConfigFileName)

    private fun defaultConfig(): Config = Config(
        emptyList(),
        emptyList(),
        null,
        null
    )

    private fun readEncryptedConfig(context: Context, file: File): Config {
        val mainKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            mainKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        val inputStream = encryptedFile.openFileInput()
        val byteArrayOutputStream = ByteArrayOutputStream()
        var nextByte: Int = inputStream.read()
        while (nextByte != -1) {
            byteArrayOutputStream.write(nextByte)
            nextByte = inputStream.read()
        }

        val json = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)
        return Config.fromJsonString(json)
    }

    fun readConfig(context: Context): Config {
        fun readConfigFile(file: File): Config {
            val bytes = file.readBytes()
            val json = String(bytes, Charsets.UTF_8)
            return Config.fromJsonString(json)
        }

        fun backupConfigFile(file: File) {
            if (file.exists()) {
                val now = Instant.now().toEpochMilli()
                val configFileBackup = context.dataDir.resolve("${configFile.name}.$now.backup")
                file.copyTo(configFileBackup)
            }
        }

        Secret.loadKey(context)

        if (configFile.exists()) {
            try {
                return readConfigFile(configFile)
            } catch (e: Exception) {
                e.printStackTrace()
                backupConfigFile(configFile)
            }
        }

        if (configFileTmp.exists()) {
            try {
                configFile.delete()
                configFileTmp.renameTo(configFile)
                return readConfigFile(configFile)
            } catch (e: Exception) {
                e.printStackTrace()
                backupConfigFile(configFile)
            }
        }

        // migrate old config
        if (encryptedConfigFile.exists()) {
            try {
                return readEncryptedConfig(context, encryptedConfigFile)
            } catch (e: Exception) {
                e.printStackTrace()
                backupConfigFile(encryptedConfigFile)
            }
        }

        return defaultConfig()
    }

    fun writeConfig(context: Context, config: Config) {
        Secret.loadKey(context)

        val json: String = config.toJsonString()
        configFileTmp.writeBytes(json.toByteArray(Charsets.UTF_8))
        configFile.delete()
        configFileTmp.renameTo(configFile)
    }
}