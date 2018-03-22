package cn.playscala.mongo.json.codecs

import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}
import play.api.libs.json.JsNumber

class JsInt32Codec extends Codec[JsNumber] {

  override def decode(reader: BsonReader, decoderContext: DecoderContext): JsNumber = {
    JsNumber(reader.readInt32())
  }

  override def encode(writer: BsonWriter, value: JsNumber, encoderContext: EncoderContext): Unit = {
    writer.writeInt32(value.value.intValue())
  }

  override def getEncoderClass: Class[JsNumber] = classOf[JsNumber]
}
