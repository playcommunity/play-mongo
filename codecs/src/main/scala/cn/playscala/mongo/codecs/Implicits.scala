package cn.playscala.mongo.codecs

import org.bson._
import org.bson.types.Decimal128
import play.api.libs.json._
import scala.collection.JavaConverters._

object Implicits {

  implicit def toBsonValue(jsValue: JsValue): BsonValue = {
    jsValue match {
      case s: JsString => new BsonString(s.value)
      case b: JsBoolean => new BsonBoolean(b.value)
      case n: JsNumber => new BsonDecimal128(new Decimal128(n.value.bigDecimal))
      case a: JsArray => new BsonArray(a.value.map(toBsonValue).asJava)
      case o: JsObject => new BsonDocument(o.fields.map(f => new BsonElement(f._1, toBsonValue(f._2))).asJava)
      case JsNull  => new BsonNull()
    }
  }
}
