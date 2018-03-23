package cn.playscala.mongo.codecs.json

import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}
import play.api.libs.json.JsString

// TODO
class JMaxKeyCodec extends Codec[JsString] {

  override def decode(reader: BsonReader, decoderContext: DecoderContext): JsString = {
    reader.readMaxKey()
    JsString("MAX_KEY")
  }

  override def encode(writer: BsonWriter, value: JsString, encoderContext: EncoderContext): Unit = {
    writer.writeMaxKey
  }

  override def getEncoderClass: Class[JsString] = classOf[JsString]
}
