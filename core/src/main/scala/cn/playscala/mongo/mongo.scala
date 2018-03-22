package cn.playscala

import scala.reflect.ClassTag

package object mongo {
  implicit def classTagToClassOf[C](ct: ClassTag[C]): Class[C] = ct.runtimeClass.asInstanceOf[Class[C]]
}
