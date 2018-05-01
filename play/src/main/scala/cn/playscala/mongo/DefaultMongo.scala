package cn.playscala.mongo

import cn.playscala.mongo.codecs.macrocodecs.ModelsRegistryProvider
import cn.playscala.mongo.gridfs.GridFSBucket
import org.bson.codecs.configuration.CodecRegistry
import scala.annotation.compileTimeOnly
import scala.language.experimental.macros


trait Mongo {

  val name: String

  val databaseName: String

  def client: MongoClient

  def database: MongoDatabase

  def gridFSBucket: GridFSBucket

  def close(): Unit

}

object Mongo {
  @compileTimeOnly("Find case classes utilises Macros and must be run at compile time.")
  def modelsRegistry(modelsPackage: String): Option[CodecRegistry] = macro ModelsRegistryProvider.modelsRegistryImpl
}

class DefaultMongo(config: MongoConfig) extends Mongo {

  override val name: String = config.name

  override val databaseName: String = config.databaseName

  lazy val lazyMongoClient: MongoClient = MongoClient(config.uri)

  lazy val lazyDatabase: MongoDatabase = lazyMongoClient.getDatabase(config.databaseName).withCodecRegistry(MongoClient.DEFAULT_CODEC_REGISTRY)

  lazy val lazyGridFSBucket: GridFSBucket = GridFSBucket(lazyDatabase)

  override def client: MongoClient = lazyMongoClient

  override def database: MongoDatabase = lazyDatabase

  override def gridFSBucket: GridFSBucket = lazyGridFSBucket

  override def close(): Unit = {
    client.close()
  }
}