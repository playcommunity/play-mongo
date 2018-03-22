package cn.playscala.mongo.json.codecs

import org.bson._
import org.bson.codecs.{Codec, DecoderContext, Encoder, EncoderContext}
import play.api.libs.json.{JsObject, JsValue}

import scala.collection.mutable.ListBuffer

class JsObjectCodec extends Codec[JsObject] {

  override def decode(reader: BsonReader, decoderContext: DecoderContext): JsObject = {
    val keyValuePairs = ListBuffer[(String, JsValue)]()

    reader.readStartDocument()

    while ( reader.readBsonType != BsonType.END_OF_DOCUMENT ) {
      val fieldName = reader.readName
      keyValuePairs += (fieldName -> readValue(reader, decoderContext))
    }

    reader.readEndDocument()

    JsObject(keyValuePairs)
  }

  protected def readValue(reader: BsonReader, decoderContext: DecoderContext): JsValue = {
    JsonCodecProvider.getCodec(reader.getCurrentBsonType).decode(reader, decoderContext)
  }


  override def encode(writer: BsonWriter, value: JsObject, encoderContext: EncoderContext): Unit = {
    writer.writeStartDocument()

    for ((field, value) <- value.fields) {
      writer.writeName(field)
      writeValue(writer, encoderContext, value)
    }

    writer.writeEndDocument()
  }

  private def writeValue(writer: BsonWriter, encoderContext: EncoderContext, value: JsValue): Unit = {
    //val codec = JsonCodecProvider.getCodec[JsValue](value.getClass.asInstanceOf[Class[JsValue]])
    val codec = JsonCodecProvider.getCodec(value.getClass)
    encoderContext.encodeWithChildContext(codec.asInstanceOf[Encoder[JsValue]], writer, value)
  }

  override def getEncoderClass: Class[JsObject] = classOf[JsObject]
}
