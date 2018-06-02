package cn.playscala.mongo.wrapper

import play.api.libs.json.{Json, OWrites}

case class DecimalWrapper(value: BigDecimal) extends NumberWrapper

case object DecimalWrapper {
  implicit val writes = OWrites[DecimalWrapper](wrapper => Json.obj("$decimal" -> wrapper.value))
}