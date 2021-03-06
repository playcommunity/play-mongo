package cn.playscala.mongo.codecs.json

import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.types.ObjectId
import org.bson.{BsonReader, BsonWriter}
import play.api.libs.json.JsString

// TODO
class JsObjectIdCodec extends Codec[JsString] {

  override def decode(reader: BsonReader, decoderContext: DecoderContext): JsString = {
    JsString(reader.readObjectId().toHexString)
  }

  override def encode(writer: BsonWriter, value: JsString, encoderContext: EncoderContext): Unit = {
    writer.writeObjectId(new ObjectId(value.value))
  }

  override def getEncoderClass: Class[JsString] = classOf[JsString]
}
