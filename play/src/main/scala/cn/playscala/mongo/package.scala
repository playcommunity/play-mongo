package cn.playscala

import java.time.Instant
import cn.playscala.mongo.wrapper.WrapperException
import org.bson.types.Decimal128
import org.bson._
import play.api.libs.json._
import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.language.implicitConversions

package object mongo {
  private val LOOSE_ISO_INSTANT_PATTERN = "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}([.][0-9]{3})?Z".r.pattern

  implicit def classTagToClassOf[C](ct: ClassTag[C]): Class[C] = ct.runtimeClass.asInstanceOf[Class[C]]

  implicit def toBsonValue(jsValue: JsValue): BsonValue = {
    jsValue match {
      case s: JsString =>
        // Convert Instant to BsonDateTime
        if (LOOSE_ISO_INSTANT_PATTERN.matcher(s.value).matches()) {
          new BsonDateTime(Instant.parse(s.value).toEpochMilli)
        } else {
          new BsonString(s.value)
        }
      case JsBoolean(b) => new BsonBoolean(b)
      //case n: JsNumber => throw WrapperException("Number type lost while using default number writes in play-json. Please don't use case classes in json dsl, and import cn.playscala.mongo.json._ to solve this problem.")
      case JsNumber(n) =>
        if (n.isValidInt) {
          new BsonInt32(n.toInt)
        } else if (n.isValidLong) {
          new BsonInt64(n.toLong)
        } else if (n.isExactDouble || n.isExactFloat) {
          new BsonDouble(n.toDouble)
        } else {
          new BsonDecimal128(new Decimal128(n.bigDecimal))
        }
      case a: JsArray => new BsonArray(a.value.map(toBsonValue).asJava)
      case o: JsObject =>
        // Recover the original value type wrapped in JsNumber.
        /*o.fields match {
          case Seq(("$int", JsNumber(n))) => new BsonInt32(n.intValue())
          case Seq(("$long", JsNumber(n))) => new BsonInt64(n.longValue())
          case Seq(("$double", JsNumber(n))) => new BsonDouble(n.doubleValue())
          case Seq(("$decimal", JsNumber(n))) => new BsonDecimal128(new Decimal128(n.bigDecimal))
          case _ => new BsonDocument(o.fields.map(f => new BsonElement(f._1, toBsonValue(f._2))).asJava)
        }*/
        new BsonDocument(o.fields.map(f => new BsonElement(f._1, toBsonValue(f._2))).asJava)
      case JsNull  => new BsonNull()
    }
  }

  implicit def toBsonDocument(jsObject: JsObject): BsonDocument = {
    //new BsonDocument(jsObject.fields.map(f => new BsonElement(f._1, toBsonValue(f._2))).asJava)
    toBsonValue(jsObject).asInstanceOf[BsonDocument]
  }

}
