package cn.playscala.mongo.codecs.macrocodecs

import java.io.File
import java.net.URLClassLoader
import java.nio.file.{Files, Paths}
import com.google.common.reflect.ClassPath
import scala.reflect.runtime.{universe => ru}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object ModelRegistryMacro {
  val CASE_CLASS_NAME_PATTERN = """case\s+class\s+(\S+)\s*\(""".r

  def modelsRegistryImpl(c: Context)(modelsPackage: c.Tree): c.Tree = {
    import c.universe._

    val q"${pkgName: String}" = modelsPackage
    val pkgSymbol = c.mirror.staticPackage(pkgName)

    /*val validCaseClassList =
      getClassNameListByGuava(pkgName)
        .filter( clazz => ru.runtimeMirror(clazz.getClassLoader).classSymbol(clazz).isCaseClass)*/

    /*val validCaseClassList =
      getClassNameListByRegex(pkgName)
        .filter( className => ru.runtimeMirror(this.getClass.getClassLoader).classSymbol(Class.forName(className)).isCaseClass)*/

    val findClassList = getClassNameList(pkgName)

    println("Find Models: " + findClassList.size)
    findClassList.foreach(c => println("Find Model: " + c))

    //val classList = findClassList.map(clazz => c.parse(s"classOf[${clazz}]"))
    val classList = findClassList.map(clazz => c.mirror.staticClass(clazz))

    val result =
      q"""
        import ${pkgSymbol}._
        import cn.playscala.mongo.codecs.Macros.createCodecProvider
        import org.bson.codecs.configuration.CodecRegistries.fromProviders
        import cn.playscala.mongo.Mongo

        if (${classList.nonEmpty}) {
          val r = fromProviders(..${classList.map(c => q"classOf[$c]")})
          Mongo.addCodecRegistry(r)
        }

        Mongo
       """
    println(show(result))
    result
  }

  /**
    * Parse all case classes from the package name using scala meta.
    * @param pkgName package name
    * @return all case classes.
    */
  private def getClassNameList(pkgName: String): List[String] = {
    import scala.meta._
    import scala.meta.contrib._
    import scala.collection.JavaConverters._

    val pkgPath = getPackagePath(pkgName)
    val pkgFile = new File(pkgPath)
    if (pkgFile.exists()) {
      Files.walk(pkgFile.toPath).iterator().asScala.toList.filter(_.toFile.getName.toLowerCase.endsWith(".scala")).flatMap{ path =>
        try {
          path.toFile.parse[Source].get
            .collect { case t: Defn.Class => t }
            .filter(c => c.mods.exists(_.is[Mod.Case]) && !c.mods.exists(_.is[Mod.Private]) && !c.mods.exists(_.is[Mod.Protected]))
            .map { c =>
              val ancestors =
                TreeOps.ancestors(c).collect {
                  case cls: Defn.Class => cls.name.value
                  case pkg: Pkg => pkg.ref.toString.trim
                  case pkgObj: Pkg.Object => pkgObj.name.value
                }

              s"${ancestors.mkString(".")}.${c.name}"
            }
        } catch { case t: Throwable =>
            println(s"Parse ${path.toFile.getAbsolutePath} error: " + t.getMessage)
            t.printStackTrace()
            Nil
        }
      }
    } else {
      println("The path of package doesn't exist: " + pkgPath)
      Nil
    }
  }

  /**
    * Get all case classes from the specified package. Lose classes sometimes.
    * @param pkgName
    * @return the full case class name list.
    */
  private def getClassNameListByGuava(pkgName: String): List[Class[_]] = {
    import scala.collection.JavaConverters._

    ClassPath
      .from(this.getClass.getClassLoader)
      .getTopLevelClasses(pkgName).asScala
      .map(c => c.load())
      //.filter(_ != "models.DummyPlaceHolder")
      .toList
  }

  // Just parse the scala file with regex expression.
  private def getClassNameListByRegex(pkgName: String): List[String] = {
    val slash = File.separator
    val basePath = System.getProperty("user.dir")
    val filePath = s"""${basePath}${slash}app${slash}${pkgName}"""
    println("find case classes in " + filePath)
    val pkgFile = new File(filePath)
    if (pkgFile.exists() && pkgFile.isDirectory) {
      val source =
        pkgFile
          .listFiles((_, name) => name.toLowerCase.endsWith(".scala")).toList
          .map(f => scala.io.Source.fromFile(f, "utf-8").getLines().mkString("\n"))
          .mkString("\n")

      CASE_CLASS_NAME_PATTERN.findAllMatchIn(source).map(m => s"${pkgName}.${m.group(1)}").toList.distinct
    } else {
      List.empty[String]
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
