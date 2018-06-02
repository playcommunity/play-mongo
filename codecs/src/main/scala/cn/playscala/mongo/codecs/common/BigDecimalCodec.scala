package cn.playscala.mongo.codecs.common

import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.types.Decimal128
import org.bson.{BsonReader, BsonWriter}

class BigDecimalCodec extends Codec[BigDecimal] {

  override def decode(reader: BsonReader, decoderContext: DecoderContext): BigDecimal = {
    reader.readDecimal128().bigDecimalValue()
  }

  override def encode(writer: BsonWriter, value: BigDecimal, encoderContext: EncoderContext): Unit = {
    writer.writeDecimal128(new Decimal128(value.bigDecimal))
  }

  override def getEncoderClass: Class[BigDecimal] = classOf[BigDecimal]
}
