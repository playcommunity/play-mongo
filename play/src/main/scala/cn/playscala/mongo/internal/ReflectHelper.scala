package cn.playscala.mongo.internal

import cn.playscala.mongo.annotations.Entity

import scala.reflect.runtime.universe._

object ReflectHelper {
  def parseCollectionName(symbol: Symbol): Option[String] = {
    symbol.annotations.find(_.tree.tpe =:= typeOf[Entity]) map { annotation =>
      val Literal(Constant(collectionName: String)) :: Nil = annotation.tree.children.tail
      collectionName
    }
  }
}
