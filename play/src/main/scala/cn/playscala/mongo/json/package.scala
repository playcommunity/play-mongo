package cn.playscala.mongo

import play.api.libs.json.{JsNumber, Json, OWrites, Writes}

import scala.language.implicitConversions

package object json {
  /*
  implicit object IntWrites extends OWrites[Int] {
    def writes(o: Int) = Json.obj("$int" -> JsNumber(o))
  }

  implicit object ShortWrites extends OWrites[Short] {
    def writes(o: Short) = Json.obj("$int" -> JsNumber(o.toInt))
  }

  implicit object ByteWrites extends OWrites[Byte] {
    def writes(o: Byte) = Json.obj("$int" -> JsNumber(o.toInt))
  }

  implicit object LongWrites extends OWrites[Long] {
    def writes(o: Long) = Json.obj("$long" -> JsNumber(o))
  }

  implicit object FloatWrites extends OWrites[Float] {
    def writes(o: Float) = Json.obj("$double" -> JsNumber(o.toDouble))
  }

  implicit object DoubleWrites extends OWrites[Double] {
    def writes(o: Double) = Json.obj("$double" -> JsNumber(o))
  }

  implicit object BigDecimalWrites extends OWrites[BigDecimal] {
    def writes(o: BigDecimal) = Json.obj("$decimal" -> JsNumber(o))
  }
  */
}
