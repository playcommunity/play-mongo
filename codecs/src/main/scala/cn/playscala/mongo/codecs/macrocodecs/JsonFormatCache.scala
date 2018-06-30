package cn.playscala.mongo.codecs.macrocodecs

import play.api.libs.json.Format

object JsonFormatCache {
  @volatile private var cache = Map[String, Format[_]]()

  def get[T](clsName: String): Option[Format[T]] = cache.get(clsName).map(_.asInstanceOf[Format[T]])

  def set[T](clsName: String, format: Format[T]) = {
    cache = cache + (clsName -> format)
  }

}
