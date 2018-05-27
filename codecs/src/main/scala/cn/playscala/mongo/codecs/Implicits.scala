package cn.playscala.mongo.codecs

import java.time.Instant
import java.time.format.DateTimeFormatter

import cn.playscala.mongo.codecs.json.JsObjectCodec
import org.bson._
import org.bson.types.Decimal128
import play.api.libs.json._

import scala.collection.JavaConverters._

object Implicits {
  private val LOOSE_ISO_INSTANT_PATTERN = "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}[.][0-9]{3}Z".r.pattern

  implicit def toBsonValue(jsValue: JsValue): BsonValue = {
    jsValue match {
      case s: JsString =>
        if (LOOSE_ISO_INSTANT_PATTERN.matcher(s.value).matches()) {
          new BsonDateTime(Instant.parse(s.value).toEpochMilli)
        } else {
          new BsonString(s.value)
        }
      case b: JsBoolean => new BsonBoolean(b.value)
      case n: JsNumber => new BsonDecimal128(new Decimal128(n.value.bigDecimal))
      case a: JsArray => new BsonArray(a.value.map(toBsonValue).asJava)
      case o: JsObject => new BsonDocument(o.fields.map(f => new BsonElement(f._1, toBsonValue(f._2))).asJava)
      case JsNull  => new BsonNull()
    }
  }

  implicit def toBsonDocument(jsObject: JsObject): BsonDocument = {
    new BsonDocument(jsObject.fields.map(f => new BsonElement(f._1, toBsonValue(f._2))).asJava)
  }

}
