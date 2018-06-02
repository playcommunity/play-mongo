package cn.playscala.mongo.wrapper

import play.api.libs.json.{Json, OWrites}

case class LongWrapper(value: Long) extends NumberWrapper

case object LongWrapper {
  implicit val writes = OWrites[LongWrapper](wrapper => Json.obj("$long" -> wrapper.value))
}
