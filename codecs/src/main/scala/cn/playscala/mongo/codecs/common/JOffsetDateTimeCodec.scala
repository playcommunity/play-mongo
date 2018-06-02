package cn.playscala.mongo.codecs.common

import java.time.{Instant, OffsetDateTime, ZoneId}

import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}

class JOffsetDateTimeCodec extends Codec[OffsetDateTime] {

  override def decode(reader: BsonReader, decoderContext: DecoderContext): OffsetDateTime = {
    OffsetDateTime.parse(reader.readString())
  }

  override def encode(writer: BsonWriter, value: OffsetDateTime, encoderContext: EncoderContext): Unit = {
    writer.writeString(value.toString)
  }

  override def getEncoderClass: Class[OffsetDateTime] = classOf[OffsetDateTime]
}
