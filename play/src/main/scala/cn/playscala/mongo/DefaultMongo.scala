package cn.playscala.mongo

import cn.playscala.mongo.gridfs.GridFSBucket

trait Mongo {

  val name: String

  val databaseName: String

  def client: MongoClient

  def database: MongoDatabase

  def gridFSBucket: GridFSBucket

  def close(): Unit
}

class DefaultMongo(config: MongoConfig) extends Mongo {

  override val name: String = config.name

  override val databaseName: String = config.databaseName

  lazy val lazyMongoClient: MongoClient = MongoClient(config.uri)

  lazy val lazyDatabase: MongoDatabase = lazyMongoClient.getDatabase(config.databaseName)

  lazy val lazyGridFSBucket: GridFSBucket = GridFSBucket(lazyDatabase)

  override def client: MongoClient = lazyMongoClient

  override def database: MongoDatabase = lazyDatabase

  override def gridFSBucket: GridFSBucket = lazyGridFSBucket

  override def close(): Unit = {
    client.close()
  }

}
