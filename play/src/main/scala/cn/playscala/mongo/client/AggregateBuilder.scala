package cn.playscala.mongo.client

import cn.playscala.mongo.{Mongo, MongoCollection}
import cn.playscala.mongo.internal.CodecHelper
import org.bson.BsonDocument
import play.api.libs.json.Json
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import cn.playscala.mongo._
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

case class AggregateBuilder[M, R] (private val collection: MongoCollection[_], field: String)(implicit mClassTag: ClassTag[M], mTypeTag: TypeTag[M], rClassTag: ClassTag[R], rTypeTag: TypeTag[R]) {

  def list(): Future[List[(M, List[R])]] = {
    val rFieldSuffix = "___"
    val rField1 = s"${field}${rFieldSuffix}"
    collection.aggregate[BsonDocument](
      Seq(
        Json.obj("$lookup" -> Json.obj(
          "from" -> Mongo.getCollectionName(rTypeTag),
          "localField" -> field,
          "foreignField" -> "_id",
          "as" -> rField1
        ))
      )
    ).map{ list =>
      list.map{ bsonDoc =>
        val model = CodecHelper.decodeBsonDocument(bsonDoc, collection.codecRegistry.get(mClassTag))
        val relateModels = bsonDoc.getArray(rField1).getValues.asScala.toList.map{ bsonVal =>
          CodecHelper.decodeBsonDocument(bsonVal.asInstanceOf[BsonDocument], collection.codecRegistry.get(rClassTag))
        }
        (model, relateModels)
      }
    }
  }

}
