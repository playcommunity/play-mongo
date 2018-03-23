package cn.playscala.mongo.codecs.json

import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}
import play.api.libs.json.JsBoolean

class JsBooleanCodec extends Codec[JsBoolean] {

  override def decode(reader: BsonReader, decoderContext: DecoderContext): JsBoolean = {
    JsBoolean(reader.readBoolean())
  }

  override def encode(writer: BsonWriter, value: JsBoolean, encoderContext: EncoderContext): Unit = {
    writer.writeBoolean(value.value)
  }

  override def getEncoderClass: Class[JsBoolean] = classOf[JsBoolean]
}
