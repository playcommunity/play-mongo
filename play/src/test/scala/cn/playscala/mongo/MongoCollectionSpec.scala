package cn.playscala.mongo

import cn.playscala.mongo.codecs.Macros._
import com.mongodb.async.client.MongoClients
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import play.api.libs.json.Json
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class MongoCollectionSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
  val codecRegistry = fromRegistries(fromProviders(classOf[Person]), MongoClient.DEFAULT_CODEC_REGISTRY, MongoClients.getDefaultCodecRegistry)
  val mongoClient: MongoClient = MongoClient("mongodb://test:123456@39.108.121.74:27117/test?authMode=scram-sha1")
  val database: MongoDatabase = mongoClient.getDatabase("test").withCodecRegistry(codecRegistry)
  val col = database.getCollection[Person]("test")

  case class Person(_id: String, name: String, age: Int)

  "MongoCollection" should "return case class result" in {
    val head = Await.result(col.find[Person](Json.obj("_id" -> "3")), 10 seconds).head
    assert(head.isInstanceOf[Person])
  }

  override protected def afterAll(): Unit = {
    mongoClient.close()
  }
}
