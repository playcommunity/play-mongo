package cn.playscala.mongo.client

import cn.playscala.mongo.{Mongo, MongoCollection}
import cn.playscala.mongo.internal.CodecHelper
import org.bson.BsonDocument
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import cn.playscala.mongo._
import com.mongodb.async.client.AggregateIterable
import org.bson.conversions.Bson
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import cn.playscala.mongo.internal.AsyncResultHelper._

case class FetchBuilder[M:ClassTag:TypeTag, R:ClassTag:TypeTag](private val wrapped: AggregateIterable[BsonDocument], field: String) {

  def list(): Future[List[(M, List[R])]] = {
    toFuture(wrapped).map{ list =>
      list.map{ bsonDoc =>
        val model = CodecHelper.decodeBsonDocument(bsonDoc, Mongo.codecRegistry.get(implicitly[ClassTag[M]]))
        val relateModels = bsonDoc.getArray(field).getValues.asScala.toList.map{ bsonVal =>
          CodecHelper.decodeBsonDocument(bsonVal.asInstanceOf[BsonDocument], Mongo.codecRegistry.get(implicitly[ClassTag[R]]))
        }
        (model, relateModels)
      }
    }
  }

}
