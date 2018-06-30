package cn.playscala.mongo.codecs.macrocodecs

import play.api.libs.json.{Format, JsValue}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

object JsonFormatMacro {
  def materializeJsonFormat[T: c.WeakTypeTag](c: Context): c.Expr[Format[T]] = {
    import c.universe._

    // Resolve full class name from parameter type.
    val tpe = c.weakTypeOf[T]
    val sym = tpe.typeSymbol

    // Resolve the package object name where the implicit macro placed.
    val q"$tpname.this.$tname" = c.prefix.tree
    val TypeName(pkgName) = tpname

    val fullClassName = show(tpe)
    if (!fullClassName.startsWith(pkgName)) {
      //println(s"${fullClassName} not classes in models.")
      c.abort(c.enclosingPosition, s"${fullClassName} is not defined in package $pkgName")
    }

    if (sym.isClass && sym.asClass.isCaseClass && !(tpe <:< typeOf[JsValue])) {
      c.Expr[Format[T]] {
        q"""
        import play.api.libs.json.Json
        import cn.playscala.mongo.codecs.macrocodecs.JsonFormatCache

        val clsName: String = ${show(tpe)}
        JsonFormatCache.get[${tpe}](clsName) match {
          case Some(f) => f
          case None =>
            val f = Json.format[${tpe}]
            JsonFormatCache.set[${tpe}](clsName, f)
            f
        }
      """
      }
    } else {
      c.Expr[Format[T]] {
        q"""
        import play.api.libs.json._
        import play.api.libs.json.Writes._
        import play.api.libs.json.Reads._
        Format(implicitly[Reads[${tpe}]], implicitly[Writes[${tpe}]])
      """
      }
    }
  }
}
