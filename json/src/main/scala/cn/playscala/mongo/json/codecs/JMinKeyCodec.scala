package cn.playscala.mongo.json.codecs

import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}
import play.api.libs.json.JsString

// TODO
class JMinKeyCodec extends Codec[JsString] {

  override def decode(reader: BsonReader, decoderContext: DecoderContext): JsString = {
    reader.readMinKey()
    JsString("MIN_KEY")
  }

  override def encode(writer: BsonWriter, value: JsString, encoderContext: EncoderContext): Unit = {
    writer.writeMinKey
  }

  override def getEncoderClass: Class[JsString] = classOf[JsString]
}
