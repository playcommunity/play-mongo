package cn.playscala.mongo.codecs.macrocodecs

import java.io.File
import java.nio.file.{Files, Paths}

import com.google.common.reflect.ClassPath

import scala.reflect.runtime.{universe => ru}
import scala.io.Source
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object ModelsRegistryProvider {
  val CASE_CLASS_NAME_PATTERN = """case\s+class\s+(\S+)\s*\(""".r

  def modelsRegistryImpl(c: Context)(modelsPackage: c.Tree): c.Tree = {
    import c.universe._

    val q"${pkgName: String}" = modelsPackage

    val pkgSymbol = c.mirror.staticPackage(pkgName)

    /*val validCaseClassList =
      getClassNameListByGuava(pkgName)
        .filter( clazz => ru.runtimeMirror(clazz.getClassLoader).classSymbol(clazz).isCaseClass)*/

    /*val validCaseClassList =
      getClassNameList(pkgName)
        .filter( className => ru.runtimeMirror(this.getClass.getClassLoader).classSymbol(Class.forName(className)).isCaseClass)*/

    val findClassList = getClassNameList(pkgName)
    findClassList.foreach(c => println("Find model: " + c))

    val classList = findClassList.map(clazz => c.parse(s"classOf[${clazz}]"))

    val res =
      q"""
        import ${pkgSymbol}._
        import cn.playscala.mongo.codecs.Macros.createCodecProvider
        import org.bson.codecs.configuration.CodecRegistries.fromProviders

        if (${classList.nonEmpty}) {
          Some(fromProviders(..${classList.map(c => q"$c")}))
        } else {
          None
        }
       """
    println(show(res))

    res
  }

  /**
    * Get all case classes from the specified package.
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

  //TODO 去除注释 ，如何处理文件编码？
  private def getClassNameList(pkgName: String): List[String] = {
    val slash = File.separator
    val basePath = System.getProperty("user.dir")
    val filePath = s"""${basePath}${slash}app${slash}${pkgName}"""
    println("find case classes in " + filePath)
    val pkgFile = new File(filePath)
    if (pkgFile.exists() && pkgFile.isDirectory) {
      val source =
        pkgFile
          .listFiles((_, name) => name.toLowerCase.endsWith(".scala")).toList
          .map(f => Source.fromFile(f, "utf-8").getLines().mkString("\n"))
          .mkString("\n")

      CASE_CLASS_NAME_PATTERN.findAllMatchIn(source).map(m => s"${pkgName}.${m.group(1)}").toList.distinct
    } else {
      List.empty[String]
    }
  }

}
