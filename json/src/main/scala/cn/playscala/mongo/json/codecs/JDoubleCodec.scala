package cn.playscala.mongo.json.codecs

import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}
import play.api.libs.json.JsNumber

class JsDoubleCodec extends Codec[JsNumber] {

  override def decode(reader: BsonReader, decoderContext: DecoderContext): JsNumber = {
    JsNumber(reader.readDouble())
  }

  override def encode(writer: BsonWriter, value: JsNumber, encoderContext: EncoderContext): Unit = {
    writer.writeDouble(value.value.doubleValue())
  }

  override def getEncoderClass: Class[JsNumber] = classOf[JsNumber]
}
