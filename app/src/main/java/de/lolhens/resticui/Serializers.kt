package de.lolhens.resticui

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.File
import java.net.URI
import java.time.Duration
import java.time.ZonedDateTime

object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor((ZonedDateTime::class).simpleName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ZonedDateTime) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): ZonedDateTime =
        ZonedDateTime.parse(decoder.decodeString())
}

data class HourDuration(val duration: Duration)

object HourDurationSerializer : KSerializer<HourDuration> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor((HourDuration::class).simpleName!!, PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: HourDuration) =
        encoder.encodeLong(value.duration.toHours())

    override fun deserialize(decoder: Decoder): HourDuration =
        HourDuration(Duration.ofHours(decoder.decodeLong()))
}

object FileSerializer : KSerializer<File> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor((File::class).simpleName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: File) = encoder.encodeString(value.path)
    override fun deserialize(decoder: Decoder): File = File(decoder.decodeString())
}

object URISerializer : KSerializer<URI> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor((URI::class).simpleName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: URI) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): URI = URI(decoder.decodeString())
}