package de.lolhens.resticui.config

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Serializable(with = SecretSerializer::class)
data class Secret(val secret: String) {
    companion object {
        private var key: AesKeyIv? = null

        fun key(): AesKeyIv = key!!

        fun loadKey(context: Context) {
            if (key != null) return

            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val keyFile = context.dataDir.resolve("key")

            key = if (keyFile.exists()) {
                val encodedKeyIv: ByteArray = readEncryptedFile(context, masterKey, keyFile)
                AesKeyIv.fromByteArray(encodedKeyIv)
            } else {
                val generatedKey = AesKeyIv.generate()
                writeEncryptedFile(context, masterKey, keyFile, generatedKey.toByteArray())
                generatedKey
            }
        }

        private fun readEncryptedFile(
            context: Context,
            masterKey: MasterKey,
            file: File
        ): ByteArray {
            val encryptedFile = EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            val inputStream = encryptedFile.openFileInput()
            val byteArrayOutputStream = ByteArrayOutputStream()
            var nextByte: Int = inputStream.read()
            while (nextByte != -1) {
                byteArrayOutputStream.write(nextByte)
                nextByte = inputStream.read()
            }

            return byteArrayOutputStream.toByteArray()
        }

        private fun writeEncryptedFile(
            context: Context,
            masterKey: MasterKey,
            file: File,
            content: ByteArray
        ) {
            val encryptedFile = EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            encryptedFile.openFileOutput().apply {
                write(content)
                flush()
                close()
            }
        }
    }

    override fun toString(): String = "*****"
}

data class AesKeyIv(val key: SecretKey, val iv: ByteArray) {
    companion object {
        private val GCM_IV_LENGTH = 12
        private val GCM_TAG_LENGTH = 16

        fun generate(): AesKeyIv {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val key = keyGenerator.generateKey()
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)
            return AesKeyIv(key, iv)
        }

        fun fromByteArray(byteArray: ByteArray): AesKeyIv {
            val encodedKey = byteArray.dropLast(GCM_IV_LENGTH).toByteArray()
            val key = SecretKeySpec(encodedKey, 0, encodedKey.size, "AES")
            val iv = byteArray.takeLast(GCM_IV_LENGTH).toByteArray()
            return AesKeyIv(key, iv)
        }
    }

    fun toByteArray(): ByteArray = key.encoded + iv

    fun encrypt(bytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH * 8, iv))
        return cipher.doFinal(bytes)
    }

    fun decrypt(bytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH * 8, iv))
        return cipher.doFinal(bytes)
    }
}

object SecretSerializer : KSerializer<Secret> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor((Secret::class).simpleName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Secret) {
        val bytes = Secret.key().encrypt(value.secret.toByteArray(Charsets.UTF_8))
        encoder.encodeString(Base64.encodeToString(bytes, Base64.DEFAULT))
    }

    override fun deserialize(decoder: Decoder): Secret {
        return try {
            val bytes = Base64.decode(decoder.decodeString(), Base64.DEFAULT)
            Secret(String(Secret.key().decrypt(bytes), Charsets.UTF_8))
        } catch (e: Exception) {
            e.printStackTrace()
            Secret("")
        }
    }
}