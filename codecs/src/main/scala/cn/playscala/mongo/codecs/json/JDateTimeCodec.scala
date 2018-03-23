package cn.playscala.mongo.codecs.json

import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}
import play.api.libs.json.JsNumber

class JDateTimeCodec extends Codec[JsNumber] {

  override def decode(reader: BsonReader, decoderContext: DecoderContext): JsNumber = {
    JsNumber(reader.readDateTime())
  }

  override def encode(writer: BsonWriter, value: JsNumber, encoderContext: EncoderContext): Unit = {
    writer.writeDateTime(value.value.longValue())
  }

  override def getEncoderClass: Class[JsNumber] = classOf[JsNumber]
}
