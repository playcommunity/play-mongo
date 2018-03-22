package cn.playscala.mongo.json.codecs

import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}
import play.api.libs.json.JsString

class JsStringCodec extends Codec[JsString] {

  override def decode(reader: BsonReader, decoderContext: DecoderContext): JsString = {
    JsString(reader.readString())
  }

  override def encode(writer: BsonWriter, value: JsString, encoderContext: EncoderContext): Unit = {
    writer.writeString(value.value)
  }

  override def getEncoderClass: Class[JsString] = classOf[JsString]
}
