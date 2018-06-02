package cn.playscala.mongo.wrapper

import play.api.libs.json.{Json, OWrites}

case class DoubleWrapper(value: Double) extends NumberWrapper

case object DoubleWrapper {
  implicit val writes = OWrites[DoubleWrapper](wrapper => Json.obj("$double" -> wrapper.value))
}
