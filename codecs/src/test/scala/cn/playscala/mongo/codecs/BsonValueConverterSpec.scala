package cn.playscala.mongo.codecs

import org.bson.{BsonArray, BsonDocument, BsonString}
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.{JsArray, Json}

class BsonValueConverterSpec extends FlatSpec with Matchers {

  "JsValue" should " be successfully converted to BsonValue" in {
    val jsValue = Json.obj("s" -> "s", "f" -> "1.1", "a" -> JsArray(Seq(Json.obj("a" -> "a"))))
    val bsValue = Implicits.toBsonValue(jsValue)
    bsValue.isInstanceOf[BsonDocument] shouldBe true
    bsValue.asInstanceOf[BsonDocument].get("s") shouldEqual new BsonString("s")
    bsValue.asInstanceOf[BsonDocument].get("a").isInstanceOf[BsonArray] shouldBe true
    bsValue.asInstanceOf[BsonDocument].get("a").asInstanceOf[BsonArray].size() shouldEqual 1
  }
}
