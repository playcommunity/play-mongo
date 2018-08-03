package cn.playscala.mongo.codecs.macrocodecs

import java.io.File
import java.net.URLClassLoader
import java.nio.file.{Files, Paths}

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object JsonFormatMacro {
  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val Apply(_, List(Literal(Constant(pkg)))) = c.prefix.tree
    println("Models pkg: " + pkg)
    val findClassList = getClassNameList(pkg.toString)
    println("Find Models: " + findClassList.size)
    findClassList.foreach(c => println("Find Model: " + c))

    val results =
      annottees.map(_.tree) match {
        case (_: ValDef) :: Nil =>
          findClassList.map{ clsName =>
            val valName = TermName(clsName.replaceAll("[.]", "_"))
            val clazz = c.mirror.staticClass(clsName)
            q"""implicit val ${valName}: play.api.libs.json.Format[${clazz}] = play.api.libs.json.Json.format[${clazz}]"""
          }
        case _ => c.abort(c.enclosingPosition, "Invalid annottee")
      }

    println("============================================")
    println(results)
    println("============================================")

    c.Expr[Any](Block(results, Literal(Constant(()))))
  }

  def getClassNameList(pkgName: String): List[String] = {
    import scala.meta._
    import scala.meta.contrib._
    import scala.collection.JavaConverters._

    def getFullClassName(c: Defn.Class): String = {
      val ancestors =
        TreeOps.ancestors(c).collect {
          case cls: Defn.Class => cls.name.value
          case pkg: Pkg => pkg.ref.toString.trim
          case pkgObj: Pkg.Object => pkgObj.name.value
        }

      s"${ancestors.mkString(".")}.${c.name}"
    }

    def getParameterTypes(tpe: Type): List[String] = {
      tpe match {
        case Type.Name(tpeName) => // simple type, e.g. String
          List(tpeName)
        case Type.Apply(Type.Name(tpeName), args) =>
          tpeName :: args.flatMap(t => getParameterTypes(t))
      }
    }

    val pkgPath = getPackagePath(pkgName)
    val pkgFile = new File(pkgPath)
    if (pkgFile.exists()) {
      val tupList =
        Files.walk(pkgFile.toPath).iterator().asScala.toList.filter(_.toFile.getName.toLowerCase.endsWith(".scala")).flatMap{ path =>
          try {
            path.toFile.parse[Source].get
              .collect { case t: Defn.Class => t }
              .filter(c => c.mods.exists(_.is[Mod.Case]) && !c.mods.exists(_.is[Mod.Private]) && !c.mods.exists(_.is[Mod.Protected]))
              .map { c =>
                val q"..$mods class $tName[..$tParams] ..$ctorMods (...$paramss) extends $template" = c
                val paramssFlat: Seq[Term.Param] = paramss.flatten

                val allParamTypes: Seq[String] = paramssFlat.flatMap { param =>
                  val nameTerm = Term.Name(param.name.value)
                  param.decltpe.getOrElse{ throw new Exception(s"type for $nameTerm not defined...") } match {
                    case Type.Name(tpeName) => // simple type, e.g. String
                      Seq(tpeName)
                    case Type.Apply(Type.Name(tpeName), args) =>
                      tpeName :: args.flatMap(t => getParameterTypes(t))
                    case other => throw new Exception(s"unable to parse $other (${other.getClass})... not (yet) supported")
                  }
                }

                (getFullClassName(c), tName.value, allParamTypes.toList.distinct, 0)
              }
          } catch { case t: Throwable =>
            println(s"Parse ${path.toFile.getAbsolutePath} error: " + t.getMessage)
            t.printStackTrace()
            Nil
          }
        }

      def calcScore(className: String): Int = {
        tupList.find(_._2 == className) match {
          case Some(tup) =>
            tup._3.filter(t => tupList.exists(_._2 == t)) match {
              case Nil => 1
              case list => 1 + list.map(l => calcScore(l)).sum[Int]
            }
          case None => 0
        }
      }

      tupList.map{ t => t.copy(_4 = calcScore(t._2)) }.sortBy(_._4).map{ t =>
        println(t)
        t._1
      }
    } else {
      println("The path of package doesn't exist: " + pkgPath)
      Nil
    }
  }

  /**
    * Get the real path of the package name.
    * @param pkgName package name
    * @return the real path of the package name
    */
  private def getPackagePath(pkgName: String): String = {
    val urls = this.getClass.getClassLoader.asInstanceOf[URLClassLoader].getURLs.toList
    urls.find(url => url.getFile.endsWith("/classes/")) match {
      case Some(url) =>
        val path = url.getFile.replace("file://", "").replace("file:/", "")
        val playPkgPath = s"${path}../../../app/${pkgName}"
        val sbtPkgPath = s"${path}../../../src/main/scala/${pkgName}"
        if(new File(playPkgPath).exists()) {
          playPkgPath
        } else {
          sbtPkgPath
        }
      case None =>
        val slash = File.separator
        val basePath = System.getProperty("user.dir")
        val playPkgPath = s"""${basePath}${slash}app${slash}${pkgName}"""
        val sbtPkgPath = s"""${basePath}${slash}src${slash}main${slash}scala${slash}${pkgName}"""
        if(new File(playPkgPath).exists()) {
          playPkgPath
        } else {
          sbtPkgPath
        }
    }
  }
}

class JsonFormat(pkg: String) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro JsonFormatMacro.impl
}
