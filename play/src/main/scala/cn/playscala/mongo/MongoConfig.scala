package cn.playscala.mongo

import com.mongodb.ConnectionString
import play.api.Configuration

object MongoConfig {
  def parse(config: Configuration): Seq[MongoConfig] = {
    val mongoConfig = config.get[Configuration]("mongodb")
    mongoConfig.keys.filter(_.endsWith("uri")).map { key =>
      val uri = mongoConfig.get[String](key)
      val dbName = new ConnectionString(uri).getDatabase
      if (key == "uri") {
        MongoConfig("default", dbName, uri)
      } else {
        MongoConfig(key.split("[.]")(0), dbName, uri)
      }
    }.toSeq
  }
}

case class MongoConfig(name: String, databaseName: String, uri: String)



