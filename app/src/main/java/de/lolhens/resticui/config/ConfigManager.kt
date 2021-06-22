package de.lolhens.resticui.config

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets

class ConfigManager(
    private val context: Context,
    fileName: String = "config.json.enc"
) {
    private val mainKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val configFile = context.dataDir.resolve(fileName)
    private val configFileTmp = context.dataDir.resolve("$fileName.tmp")

    private fun defaultConfig(): Config = Config(
        emptyList(),
        emptyList()
    )

    private fun readConfig(file: File): Config {
        val encryptedFile = EncryptedFile.Builder(
            context,
            configFile,
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

    fun readConfig(): Config {
        if (configFile.exists()) try {
            return readConfig(configFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (configFileTmp.exists()) try {
            configFile.delete()
            configFileTmp.renameTo(configFile)
            return readConfig(configFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return defaultConfig()
    }

    fun writeConfig(config: Config) {
        val createBackup = configFile.exists()

        if (createBackup) {
            configFileTmp.delete()
            configFile.renameTo(configFileTmp)
        }

        val encryptedFile = EncryptedFile.Builder(
            context,
            configFile,
            mainKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        val json: String = config.toJsonString()

        encryptedFile.openFileOutput().apply {
            write(json.toByteArray(StandardCharsets.UTF_8))
            flush()
            close()
        }

        if (createBackup) {
            configFileTmp.delete()
        }
    }
}