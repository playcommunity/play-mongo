package cn.playscala.mongo

import javax.inject.Singleton

import cn.playscala.mongo.{MongoClient, MongoConfig, MongoDatabase}
import cn.playscala.mongo.annotations.Entity
import cn.playscala.mongo.client.FindBuilder
import cn.playscala.mongo.codecs.json.JsonCodecProvider
import cn.playscala.mongo.codecs.macrocodecs.ModelsRegistryProvider
import cn.playscala.mongo.codecs.time.JOffsetDateTimeCodec
import cn.playscala.mongo.gridfs.GridFSBucket
import cn.playscala.mongo.internal.DefaultHelper.DefaultsTo
import com.mongodb.async.client.MongoClients
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromProviders, fromRegistries}
import play.api.libs.json.JsObject

import scala.annotation.compileTimeOnly
import scala.language.experimental.macros
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

object Mongo {
  @volatile
  private var collectionNameMap: Map[String, String] = Map.empty[String, String]

  var isDevMode = false

  private var codecRegistry: CodecRegistry = fromRegistries(
    MongoClients.getDefaultCodecRegistry,
    fromProviders(new JsonCodecProvider()),
    fromCodecs(new JOffsetDateTimeCodec)
  )

  def addCodecRegistry(registry: CodecRegistry): Mongo.type = {
    codecRegistry = fromRegistries(codecRegistry, registry)
    this
  }

  def getCollectionName(typeTag: TypeTag[_]): String = {
    val fullClassName = typeTag.tpe.typeSymbol.fullName

    if (!isDevMode && collectionNameMap.contains(fullClassName)) {
      collectionNameMap(fullClassName)
    } else {
      val parsedName =
        typeTag.tpe.typeSymbol.annotations.find(_.tree.tpe =:= typeOf[Entity]).map{ annotation =>
          val Literal(Constant(collectionName: String)) :: Nil = annotation.tree.children.tail
          collectionName
        }.getOrElse(typeTag.tpe.typeSymbol.name.toString)

      collectionNameMap += (fullClassName -> parsedName)
      parsedName
    }
  }

  @compileTimeOnly("Find case classes utilises Macros and must be run at compile time.")
  def setModelsPackage(modelsPackage: String): Mongo.type = macro ModelsRegistryProvider.modelsRegistryImpl
}

case class Mongo(config: MongoConfig) {
  import Mongo._

  val name: String = config.name

  val databaseName: String = config.databaseName

  val client: MongoClient = MongoClient(config.uri)

  val database: MongoDatabase = client.getDatabase(config.databaseName).withCodecRegistry(codecRegistry)

  val gridFSBucket: GridFSBucket = GridFSBucket(database)

  /**
    * Finds all documents in the collection.
    *
    * [[http://docs.mongodb.org/manual/tutorial/query-documents/ Find]]
    *
    * @tparam M   the target document type.
    * @return the FindBuilder
    */
  def find[M]()(implicit ct: ClassTag[M], tt: TypeTag[M]): FindBuilder[M, M] = {
    getCollection[M].find[M]()
  }

  /**
    * Finds all documents in the collection.
    *
    * [[http://docs.mongodb.org/manual/tutorial/query-documents/ Find]]
    * @param filter the query filter
    * @tparam M    the target document type.
    * @return the future of result list.
    */
  def find[M](filter: JsObject)(implicit ct: ClassTag[M], tt: TypeTag[M]): FindBuilder[M, M] = {
    getCollection[M].find[M](filter)
  }

  /**
    * Get the collection according to it's TypeTag.
    * @param ct
    * @param tt
    * @tparam M
    * @return
    */
  private def getCollection[M](implicit ct: ClassTag[M], tt: TypeTag[M]): MongoCollection[M] =
    database.getCollection[M](Mongo.getCollectionName(tt))


  def close(): Unit = {
    client.close()
  }
}
