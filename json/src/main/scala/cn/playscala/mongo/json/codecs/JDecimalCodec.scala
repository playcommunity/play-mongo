package cn.playscala.mongo.json.codecs

import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.types.Decimal128
import org.bson.{BsonReader, BsonWriter}
import play.api.libs.json.JsNumber

class JsNumberCodec extends Codec[JsNumber] {

  override def decode(reader: BsonReader, decoderContext: DecoderContext): JsNumber = {
    JsNumber(reader.readDecimal128().bigDecimalValue())
  }

  override def encode(writer: BsonWriter, value: JsNumber, encoderContext: EncoderContext): Unit = {
    writer.writeDecimal128(new Decimal128(value.value.bigDecimal))
  }

  override def getEncoderClass: Class[JsNumber] = classOf[JsNumber]
}
