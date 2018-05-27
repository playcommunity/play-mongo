package cn.playscala.mongo.codecs.json

import org.bson.BsonType
import org.bson.BsonType.{DECIMAL128, _}
import org.bson.codecs.Codec
import org.bson.codecs.configuration.{CodecProvider, CodecRegistry}
import play.api.libs.json.{JsNull, _}

/**
  * A {@code CodecProvider} for all subclass of JsValue.
  *
  * @since 0.1.0
  */

object JsonCodecProvider {

  // Response for decoding BsonType to JsValue
  private val BSON_TYPE_CODEC_MAP : Map[BsonType, Codec[_]] = Map (
    NULL -> new JsNullCodec,
    ARRAY -> new JsArrayCodec,
    BINARY -> new JBinaryCodec,
    BOOLEAN -> new JsBooleanCodec,
    DATE_TIME -> new JDateTimeCodec,
    DOCUMENT -> new JsObjectCodec,
    DOUBLE -> new JsDoubleCodec,
    INT32 -> new JsInt32Codec,
    INT64 -> new JsInt64Codec,
    DECIMAL128 -> new JDecimalCodec,
    MAX_KEY -> new JMaxKeyCodec,
    MIN_KEY -> new JMinKeyCodec,
    JAVASCRIPT -> new JJavaScriptCodec,
    JAVASCRIPT_WITH_SCOPE -> new JJavaScriptWithScopeCodec,
    OBJECT_ID -> new JsObjectIdCodec,
    REGULAR_EXPRESSION -> new JRegularExpressionCodec,
    STRING -> new JsStringCodec,
    SYMBOL -> new JSymbolCodec,
    TIMESTAMP -> new JTimestampCodec,
    UNDEFINED -> new JUndefinedCodec
  )

  // Response for encoding JsValue to BsonValue
  private val JSON_CLASS_CODEC_MAP: Map[Class[_], Codec[_]] = Map (
    JsNull.getClass.asInstanceOf[Class[JsNull.type]] -> new JsNullCodec,
    classOf[JsArray] -> new JsArrayCodec,
    classOf[JsBoolean] -> new JsBooleanCodec,
    classOf[JsNumber] -> new JDecimalCodec,
    classOf[JsObject] -> new JsObjectCodec,
    classOf[JsString] -> new JsStringCodec
  )

  /*
  private val cn.playscala.mongo.codecs: Map[Class[_], Codec[_]] = Map (
    JsNull.getClass.asInstanceOf[Class[JsNull.type ]] -> new JsNullCodec,
    classOf[JsArray] -> new JsArrayCodec,
    classOf[JsNumber] -> new JsNumberCodec,
    classOf[JsString] -> new JsStringCodec,
    classOf[JsObject] -> new JsObjectCodec,
    classOf[BsonDocument] -> new BsonDocumentCodec()
    //classOf[Document] -> new DocumentCodec()
  )


  private val BSON_TYPE_CLASS_MAP = Map (
    BsonType.NULL -> JsNull.getClass,
    BsonType.ARRAY -> classOf[JsArray],
    //BsonType.ARRAY -> classOf[?]
    BsonType.BOOLEAN -> classOf[JsBoolean],
    //BsonType.DATE_TIME -> classOf[?],
    //BsonType.DB_POINTER -> classOf[?],
    BsonType.DOCUMENT -> classOf[JsObject],
    BsonType.DOUBLE -> classOf[JsNumber],
    BsonType.INT32 -> classOf[JsNumber],
    BsonType.INT64 -> classOf[JsNumber],
    BsonType.DECIMAL128 -> classOf[JsNumber],
    //BsonType.MAX_KEY -> classOf[?],
    //BsonType.MIN_KEY -> classOf[?],
    BsonType.DECIMAL128 -> classOf[JsNumber],
    //BsonType.JAVASCRIPT -> classOf[?],
    //BsonType.JAVASCRIPT_WITH_SCOPE -> classOf[?],
    //BsonType.OBJECT_ID -> classOf[?],
    //BsonType.REGULAR_EXPRESSION -> classOf[?],
    BsonType.STRING -> classOf[JsString]
    //BsonType.SYMBOL -> classOf[?],
    //BsonType.TIMESTAMP -> classOf[?],
    //BsonType.UNDEFINED -> classOf[?],
  )
  */

  //def getCodec(bsonType: BsonType): Codec[JsValue] = cn.playscala.mongo.codecs(BSON_TYPE_CLASS_MAP(bsonType)).asInstanceOf[Codec[JsValue]]
  def getCodec[T](bsonType: BsonType): Codec[T] = {
    BSON_TYPE_CODEC_MAP(bsonType).asInstanceOf[Codec[T]]
  }

  //def getCodec(clazz: Class[_]): Codec[JsValue] = cn.playscala.mongo.codecs(clazz).asInstanceOf[Codec[JsValue]]
  def getCodec[T](clazz: Class[T]): Codec[T] = {
    JSON_CLASS_CODEC_MAP.get(clazz).map(_.asInstanceOf[Codec[T]]).getOrElse(null)
  }
}

class JsonCodecProvider extends CodecProvider {
  override def get[T](clazz: Class[T], registry: CodecRegistry): Codec[T] = {
    //JsonCodecProvider.cn.playscala.mongo.codecs(clazz).asInstanceOf[Codec[T]]
    JsonCodecProvider.getCodec(clazz)
  }
}
