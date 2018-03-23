package cn.playscala.mongo.codecs.json

import org.bson.codecs.{Codec, DecoderContext, Encoder, EncoderContext}
import org.bson.{BsonReader, BsonType, BsonWriter}
import play.api.libs.json.{JsArray, JsValue}

import scala.collection.mutable.ListBuffer

class JsArrayCodec extends Codec[JsArray] {

  override def decode(reader: BsonReader, decoderContext: DecoderContext): JsArray = {
    val list = new ListBuffer[JsValue]

    reader.readStartArray()

    while (reader.readBsonType != BsonType.END_OF_DOCUMENT) {
      list += readValue(reader, decoderContext)
    }

    reader.readEndArray()

    JsArray(list.toList)
  }

  /**
    * This method may be overridden to change the behavior of reading the current value from the given {@code BsonReader}.  It is required
    * that the value be fully consumed before returning.
    *
    * @param reader         the read to read the value from
    * @param decoderContext the decoder context
    * @return the non-null value read from the reader
    */
  protected def readValue(reader: BsonReader, decoderContext: DecoderContext): JsValue =
    JsonCodecProvider.getCodec(reader.getCurrentBsonType).decode(reader, decoderContext)

  override def encode(writer: BsonWriter, value: JsArray, encoderContext: EncoderContext): Unit = {
    writer.writeStartArray()

    for (jsValue <- value.value) {
      val codec = JsonCodecProvider.getCodec(jsValue.getClass)
      encoderContext.encodeWithChildContext(codec.asInstanceOf[Encoder[JsValue]], writer, jsValue)
    }

    writer.writeEndArray()
  }

  override def getEncoderClass: Class[JsArray] = classOf[JsArray]
}
