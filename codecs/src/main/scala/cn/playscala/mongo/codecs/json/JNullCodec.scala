package cn.playscala.mongo.codecs.json

import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}
import play.api.libs.json.JsNull

class JsNullCodec extends Codec[JsNull.type] {
  override def decode(reader: BsonReader, decoderContext: DecoderContext): JsNull.type = {
    reader.readNull()
    JsNull
  }

  override def encode(writer: BsonWriter, value: JsNull.type, encoderContext: EncoderContext): Unit = {
    writer.writeNull()
  }

  override def getEncoderClass: Class[JsNull.type] = JsNull.getClass.asInstanceOf[Class[JsNull.type]]
}
