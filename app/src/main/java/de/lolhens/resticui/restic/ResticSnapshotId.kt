package de.lolhens.resticui.restic

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ResticSnapshotIdSerializer::class)
data class ResticSnapshotId(val id: String) {
    val short get() = id.take(8)
}

object ResticSnapshotIdSerializer : KSerializer<ResticSnapshotId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor((ResticSnapshotId::class).simpleName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ResticSnapshotId) =
        encoder.encodeString(value.id)

    override fun deserialize(decoder: Decoder): ResticSnapshotId =
        ResticSnapshotId(decoder.decodeString())
}