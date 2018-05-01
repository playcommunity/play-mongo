package cn.playscala

import scala.reflect.ClassTag
import scala.language.implicitConversions

package object mongo {
  implicit def classTagToClassOf[C](ct: ClassTag[C]): Class[C] = ct.runtimeClass.asInstanceOf[Class[C]]

  implicit val findClasses: String => List[Class[_]] = (pkg: String) => {
    List.empty[Class[_]]
  }
}
