package de.lolhens.resticui

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.long
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

object DurationSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Custom${(Duration::class).simpleName!!}", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Duration) =
        encoder.encodeString("${value.toMillis()}ms")

    override fun deserialize(decoder: Decoder): Duration {
        val value = decoder.decodeSerializableValue(JsonPrimitive.serializer())
        if (value.isString && value.content.endsWith("ms")) {
            return Duration.ofMillis(value.content.dropLast(2).toLong())
        } else {
            return Duration.ofHours(value.long)
        }
    }
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