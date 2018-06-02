package cn.playscala.mongo.wrapper

import play.api.libs.json.{Json, OWrites}

case class IntWrapper(value: Int) extends NumberWrapper

case object IntWrapper {
  implicit val writes = OWrites[LongWrapper](wrapper => Json.obj("$int" -> wrapper.value))
}
