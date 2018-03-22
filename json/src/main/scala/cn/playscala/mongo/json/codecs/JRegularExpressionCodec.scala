package cn.playscala.mongo.json.codecs

import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonRegularExpression, BsonWriter}
import play.api.libs.json.JsString

// TODO
class JRegularExpressionCodec extends Codec[JsString] {

  override def decode(reader: BsonReader, decoderContext: DecoderContext): JsString = {
    JsString(reader.readRegularExpression().getPattern)
  }

  override def encode(writer: BsonWriter, value: JsString, encoderContext: EncoderContext): Unit = {
    writer.writeRegularExpression(new BsonRegularExpression(value.value))
  }

  override def getEncoderClass: Class[JsString] = classOf[JsString]
}
