package de.lolhens.resticui.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

@Serializable(with = FolderConfigIdSerializer::class)
data class FolderConfigId(val uuid: UUID) {
    companion object {
        fun create(): FolderConfigId = FolderConfigId(UUID.randomUUID())

        fun fromString(string: String): FolderConfigId = FolderConfigId(UUID.fromString(string))
    }

    override fun toString(): String = uuid.toString()
}

object FolderConfigIdSerializer : KSerializer<FolderConfigId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor((FolderConfigId::class).simpleName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: FolderConfigId) =
        encoder.encodeString(value.uuid.toString())

    override fun deserialize(decoder: Decoder): FolderConfigId =
        FolderConfigId(UUID.fromString(decoder.decodeString()))
}