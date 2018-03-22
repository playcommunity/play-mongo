package cn.playscala.mongo.json.codecs

import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonBinary, BsonReader, BsonWriter}
import play.api.libs.json.JsString

// TODO
class JBinaryCodec extends Codec[JsString] {

  override def decode(reader: BsonReader, decoderContext: DecoderContext): JsString = {
    reader.readBinaryData()
    JsString("<Binary>")
  }

  override def encode(writer: BsonWriter, value: JsString, encoderContext: EncoderContext): Unit = {
    writer.writeBinaryData(new BsonBinary("<binary>".getBytes))
  }

  override def getEncoderClass: Class[JsString] = classOf[JsString]
}
