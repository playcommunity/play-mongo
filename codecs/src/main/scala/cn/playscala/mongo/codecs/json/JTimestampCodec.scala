package cn.playscala.mongo.codecs.json

import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonTimestamp, BsonWriter}
import play.api.libs.json.JsNumber

class JTimestampCodec extends Codec[JsNumber] {

  override def decode(reader: BsonReader, decoderContext: DecoderContext): JsNumber = {
    JsNumber(reader.readTimestamp().getValue)
  }

  override def encode(writer: BsonWriter, value: JsNumber, encoderContext: EncoderContext): Unit = {
    writer.writeTimestamp(new BsonTimestamp(value.value.longValue()))
  }

  override def getEncoderClass: Class[JsNumber] = classOf[JsNumber]
}
