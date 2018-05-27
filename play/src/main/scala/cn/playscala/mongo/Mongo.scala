package cn.playscala.mongo

import javax.inject.Singleton

import cn.playscala.mongo.{MongoClient, MongoConfig, MongoDatabase}
import cn.playscala.mongo.annotations.Entity
import cn.playscala.mongo.client.{ClientSession, FindBuilder}
import cn.playscala.mongo.codecs.IterableCodecProvider
import cn.playscala.mongo.codecs.json.JsonCodecProvider
import cn.playscala.mongo.codecs.macrocodecs.ModelsRegistryProvider
import cn.playscala.mongo.codecs.time.JOffsetDateTimeCodec
import cn.playscala.mongo.gridfs.GridFSBucket
import cn.playscala.mongo.internal.AsyncResultHelper.toFuture
import cn.playscala.mongo.internal.DefaultHelper.DefaultsTo
import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.MongoClients
import com.mongodb.client.model._
import com.mongodb.client.result.{DeleteResult, UpdateResult}
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromProviders, fromRegistries}
import org.bson.conversions.Bson
import play.api.libs.json.JsObject

import scala.annotation.compileTimeOnly
import scala.concurrent.Future
import scala.language.experimental.macros
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.{Constant, Literal, TypeTag, typeOf}

object Mongo {
  @volatile private var collectionNameMap: Map[String, String] = Map.empty[String, String]

  var isDevMode = false

  @volatile var codecRegistry: CodecRegistry = fromRegistries(
    MongoClients.getDefaultCodecRegistry,
    fromProviders(new JsonCodecProvider()),
    fromCodecs(new JOffsetDateTimeCodec),
    fromProviders(IterableCodecProvider())
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
  def find[M]()(implicit ct: ClassTag[M], tt: TypeTag[M]): FindBuilder[M] = {
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
  def find[M](filter: JsObject)(implicit ct: ClassTag[M], tt: TypeTag[M]): FindBuilder[M] = {
    getCollection[M].find[M](filter)
  }

  /**
    * Finds all documents in the collection.
    *
    * [[http://docs.mongodb.org/manual/tutorial/query-documents/ Find]]
    * @param filter the query filter
    * @tparam M    the target document type.
    * @return the future of result list.
    */
  def find[M](filter: JsObject, projection: JsObject)(implicit ct: ClassTag[M], tt: TypeTag[M]): FindBuilder[M] = {
    getCollection[M].find[M](filter, projection)
  }

  /**
    * Finds all documents in the collection.
    *
    * [[http://docs.mongodb.org/manual/tutorial/query-documents/ Find]]
    *
    * @param clientSession the client session with which to associate this operation
    * @tparam M  the target document type of the observable.
    * @return the find Observable
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def find[M:ClassTag:TypeTag](clientSession: ClientSession): FindBuilder[M] = {
    getCollection[M].find[M](clientSession)
  }

  /**
    * Finds all documents in the collection.
    *
    * [[http://docs.mongodb.org/manual/tutorial/query-documents/ Find]]
    * @param clientSession the client session with which to associate this operation
    * @param filter the query filter
    * @tparam M    the target document type of the observable.
    * @return the find Observable
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def find[M:ClassTag:TypeTag](clientSession: ClientSession, filter: JsObject): FindBuilder[M] = {
    getCollection[M].find[M](clientSession, filter)
  }

  /**
    * Finds all documents in the collection.
    *
    * [[http://docs.mongodb.org/manual/tutorial/query-documents/ Find]]
    * @param clientSession the client session with which to associate this operation
    * @param filter the query filter
    * @tparam M    the target document type of the observable.
    * @return the find Observable
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def find[M:ClassTag:TypeTag](clientSession: ClientSession, filter: JsObject, projection: JsObject): FindBuilder[M] = {
    getCollection[M].find[M](clientSession, filter, projection)
  }

  /**
    * Inserts the provided document. If the document is missing an identifier, the driver should generate one.
    *
    * @param document the document to insert
    * @return a Observable with a single element indicating when the operation has completed or with either a
    *         com.mongodb.DuplicateKeyException or com.mongodb.MongoException
    */
  def insertOne[M:ClassTag:TypeTag](document: M): Future[Void] =
    getCollection[M].insertOne(document)

  /**
    * Inserts the provided document. If the document is missing an identifier, the driver should generate one.
    *
    * @param document the document to insert
    * @param options  the options to apply to the operation
    * @return a Observable with a single element indicating when the operation has completed or with either a
    *         com.mongodb.DuplicateKeyException or com.mongodb.MongoException
    * @since 1.1
    */
  def insertOne[M:ClassTag:TypeTag](document: M, options: InsertOneOptions): Future[Void] =
    getCollection[M].insertOne(document, options)

  /**
    * Inserts the provided document. If the document is missing an identifier, the driver should generate one.
    *
    * @param clientSession the client session with which to associate this operation
    * @param document the document to insert
    * @return a Observable with a single element indicating when the operation has completed or with either a
    *         com.mongodb.DuplicateKeyException or com.mongodb.MongoException
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def insertOne[M:ClassTag:TypeTag](clientSession: ClientSession, document: M): Future[Void] =
    getCollection[M].insertOne(clientSession, document)

  /**
    * Inserts the provided document. If the document is missing an identifier, the driver should generate one.
    *
    * @param clientSession the client session with which to associate this operation
    * @param document the document to insert
    * @param options  the options to apply to the operation
    * @return a Observable with a single element indicating when the operation has completed or with either a
    *         com.mongodb.DuplicateKeyException or com.mongodb.MongoException
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def insertOne[M:ClassTag:TypeTag](clientSession: ClientSession, document: M, options: InsertOneOptions): Future[Void] =
    getCollection[M].insertOne(clientSession, document, options)

  /**
    * Inserts a batch of documents. The preferred way to perform bulk inserts is to use the BulkWrite API. However, when talking with a
    * server &lt; 2.6, using this method will be faster due to constraints in the bulk API related to error handling.
    *
    * @param documents the documents to insert
    * @return a Observable with a single element indicating when the operation has completed or with either a
    *         com.mongodb.DuplicateKeyException or com.mongodb.MongoException
    */
  def insertMany[M:ClassTag:TypeTag](documents: Seq[_ <: M]): Future[Void] =
    getCollection[M].insertMany(documents)

  /**
    * Inserts a batch of documents. The preferred way to perform bulk inserts is to use the BulkWrite API. However, when talking with a
    * server &lt; 2.6, using this method will be faster due to constraints in the bulk API related to error handling.
    *
    * @param documents the documents to insert
    * @param options   the options to apply to the operation
    * @return a Observable with a single element indicating when the operation has completed or with either a
    *         com.mongodb.DuplicateKeyException or com.mongodb.MongoException
    */
  def insertMany[M:ClassTag:TypeTag](documents: Seq[_ <: M], options: InsertManyOptions): Future[Void] =
    getCollection[M].insertMany(documents, options)

  /**
    * Inserts a batch of documents. The preferred way to perform bulk inserts is to use the BulkWrite API.
    *
    * @param clientSession the client session with which to associate this operation
    * @param documents the documents to insert
    * @return a Observable with a single element indicating when the operation has completed or with either a
    *         com.mongodb.DuplicateKeyException or com.mongodb.MongoException
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def insertMany[M:ClassTag:TypeTag](clientSession: ClientSession, documents: Seq[_ <: M]): Future[Void] =
    getCollection[M].insertMany(clientSession, documents)

  /**
    * Inserts a batch of documents. The preferred way to perform bulk inserts is to use the BulkWrite API.
    *
    * @param clientSession the client session with which to associate this operation
    * @param documents the documents to insert
    * @param options   the options to apply to the operation
    * @return a Observable with a single element indicating when the operation has completed or with either a
    *         com.mongodb.DuplicateKeyException or com.mongodb.MongoException
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def insertMany[M:ClassTag:TypeTag](clientSession: ClientSession, documents: Seq[_ <: M], options: InsertManyOptions): Future[Void] =
    getCollection[M].insertMany(clientSession, documents, options)
  
  /**
    * Update a single document in the collection according to the specified arguments.
    *
    * [[http://docs.mongodb.org/manual/tutorial/modify-documents/ Updates]]
    * [[http://docs.mongodb.org/manual/reference/operator/update/ Update Operators]]
    * @param filter  a document describing the query filter, which may not be null. This can be of any type for which a `Codec` is
    *                registered
    * @param update  a document describing the update, which may not be null. The update to apply must include only update operators. This
    *                can be of any type for which a `Codec` is registered
    * @return a Observable with a single element the UpdateResult
    */
  def updateOne[M:ClassTag:TypeTag](filter: JsObject, update: JsObject): Future[UpdateResult] =
    getCollection[M].updateOne(filter, update)

  /**
    * Update a single document in the collection according to the specified arguments.
    *
    * [[http://docs.mongodb.org/manual/tutorial/modify-documents/ Updates]]
    * [[http://docs.mongodb.org/manual/reference/operator/update/ Update Operators]]
    * @param filter  a document describing the query filter, which may not be null. This can be of any type for which a `Codec` is
    *                registered
    * @param update  a document describing the update, which may not be null. The update to apply must include only update operators. This
    *                can be of any type for which a `Codec` is registered
    * @param options the options to apply to the update operation
    * @return a Observable with a single element the UpdateResult
    */
  def updateOne[M:ClassTag:TypeTag](filter: JsObject, update: JsObject, options: UpdateOptions): Future[UpdateResult] =
    getCollection[M].updateOne(filter, update, options)

  /**
    * Update a single document in the collection according to the specified arguments.
    *
    * [[http://docs.mongodb.org/manual/tutorial/modify-documents/ Updates]]
    * [[http://docs.mongodb.org/manual/reference/operator/update/ Update Operators]]
    * @param clientSession the client session with which to associate this operation
    * @param filter  a document describing the query filter, which may not be null. This can be of any type for which a `Codec` is
    *                registered
    * @param update  a document describing the update, which may not be null. The update to apply must include only update operators. This
    *                can be of any type for which a `Codec` is registered
    * @return a Observable with a single element the UpdateResult
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def updateOne[M:ClassTag:TypeTag](clientSession: ClientSession, filter: JsObject, update: JsObject): Future[UpdateResult] =
    getCollection[M].updateOne(clientSession, filter, update)

  /**
    * Update a single document in the collection according to the specified arguments.
    *
    * [[http://docs.mongodb.org/manual/tutorial/modify-documents/ Updates]]
    * [[http://docs.mongodb.org/manual/reference/operator/update/ Update Operators]]
    * @param clientSession the client session with which to associate this operation
    * @param filter  a document describing the query filter, which may not be null. This can be of any type for which a `Codec` is
    *                registered
    * @param update  a document describing the update, which may not be null. The update to apply must include only update operators. This
    *                can be of any type for which a `Codec` is registered
    * @param options the options to apply to the update operation
    * @return a Observable with a single element the UpdateResult
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def updateOne[M:ClassTag:TypeTag](clientSession: ClientSession, filter: JsObject, update: JsObject, options: UpdateOptions): Future[UpdateResult] =
    getCollection[M].updateOne(clientSession, filter, update, options)

  /**
    * Update many documents in the collection according to the specified arguments.
    *
    * [[http://docs.mongodb.org/manual/tutorial/modify-documents/ Updates]]
    * [[http://docs.mongodb.org/manual/reference/operator/update/ Update Operators]]
    * @param filter  a document describing the query filter, which may not be null. This can be of any type for which a `Codec` is
    *                registered
    * @param update  a document describing the update, which may not be null. The update to apply must include only update operators. This
    *                can be of any type for which a `Codec` is registered
    * @return a Future with a single element the UpdateResult
    */
  def updateMany[M:ClassTag:TypeTag](filter: JsObject, update: JsObject): Future[UpdateResult] =
    getCollection[M].updateMany(filter, update)

  /**
    * Update a single document in the collection according to the specified arguments.
    *
    * [[http://docs.mongodb.org/manual/tutorial/modify-documents/ Updates]]
    * [[http://docs.mongodb.org/manual/reference/operator/update/ Update Operators]]
    * @param filter  a document describing the query filter, which may not be null. This can be of any type for which a `Codec` is
    *                registered
    * @param update  a document describing the update, which may not be null. The update to apply must include only update operators. This
    *                can be of any type for which a `Codec` is registered
    * @param options the options to apply to the update operation
    * @return a Observable with a single element the UpdateResult
    */
  def updateMany[M:ClassTag:TypeTag](filter: JsObject, update: JsObject, options: UpdateOptions): Future[UpdateResult] =
    getCollection[M].updateMany(filter, update, options)

  /**
    * Update a single document in the collection according to the specified arguments.
    *
    * [[http://docs.mongodb.org/manual/tutorial/modify-documents/ Updates]]
    * [[http://docs.mongodb.org/manual/reference/operator/update/ Update Operators]]
    * @param clientSession the client session with which to associate this operation
    * @param filter  a document describing the query filter, which may not be null. This can be of any type for which a `Codec` is
    *                registered
    * @param update  a document describing the update, which may not be null. The update to apply must include only update operators. This
    *                can be of any type for which a `Codec` is registered
    * @return a Observable with a single element the UpdateResult
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def updateMany[M:ClassTag:TypeTag](clientSession: ClientSession, filter: JsObject, update: JsObject): Future[UpdateResult] =
    getCollection[M].updateMany(clientSession, filter, update)

  /**
    * Update a single document in the collection according to the specified arguments.
    *
    * [[http://docs.mongodb.org/manual/tutorial/modify-documents/ Updates]]
    * [[http://docs.mongodb.org/manual/reference/operator/update/ Update Operators]]
    * @param clientSession the client session with which to associate this operation
    * @param filter  a document describing the query filter, which may not be null. This can be of any type for which a `Codec` is
    *                registered
    * @param update  a document describing the update, which may not be null. The update to apply must include only update operators. This
    *                can be of any type for which a `Codec` is registered
    * @param options the options to apply to the update operation
    * @return a Observable with a single element the UpdateResult
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def updateMany[M:ClassTag:TypeTag](clientSession: ClientSession, filter: JsObject, update: JsObject, options: UpdateOptions): Future[UpdateResult] =
    getCollection[M].updateMany(clientSession, filter, update, options)

  /**
    * Removes at most one document from the collection that matches the given filter.  If no documents match, the collection is not
    * modified.
    *
    * @param filter the query filter to apply the the delete operation
    * @return a Observable with a single element the DeleteResult or with an com.mongodb.MongoException
    */
  def deleteOne[M:ClassTag:TypeTag](filter: JsObject): Future[DeleteResult] =
    getCollection[M].deleteOne(filter)

  /**
    * Removes at most one document from the collection that matches the given filter.  If no documents match, the collection is not
    * modified.
    *
    * @param filter the query filter to apply the the delete operation
    * @param options the options to apply to the delete operation
    * @return a Observable with a single element the DeleteResult or with an com.mongodb.MongoException
    * @since 1.2
    */
  def deleteOne[M:ClassTag:TypeTag](filter: JsObject, options: DeleteOptions): Future[DeleteResult] =
    getCollection[M].deleteOne(filter, options)

  /**
    * Removes at most one document from the collection that matches the given filter.  If no documents match, the collection is not
    * modified.
    *
    * @param clientSession the client session with which to associate this operation
    * @param filter the query filter to apply the the delete operation
    * @return a Observable with a single element the DeleteResult or with an com.mongodb.MongoException
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def deleteOne[M:ClassTag:TypeTag](clientSession: ClientSession, filter: JsObject): Future[DeleteResult] =
    getCollection[M].deleteOne(clientSession, filter)

  /**
    * Removes at most one document from the collection that matches the given filter.  If no documents match, the collection is not
    * modified.
    *
    * @param clientSession the client session with which to associate this operation
    * @param filter the query filter to apply the the delete operation
    * @param options the options to apply to the delete operation
    * @return a Observable with a single element the DeleteResult or with an com.mongodb.MongoException
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def deleteOne[M:ClassTag:TypeTag](clientSession: ClientSession, filter: JsObject, options: DeleteOptions): Future[DeleteResult] =
    getCollection[M].deleteOne(clientSession, filter, options)

  /**
    * Removes all documents from the collection that match the given query filter.  If no documents match, the collection is not modified.
    *
    * @param filter the query filter to apply the the delete operation
    * @return a Observable with a single element the DeleteResult or with an com.mongodb.MongoException
    */
  def deleteMany[M:ClassTag:TypeTag](filter: JsObject): Future[DeleteResult] =
    getCollection[M].deleteMany(filter)

  /**
    * Removes all documents from the collection that match the given query filter.  If no documents match, the collection is not modified.
    *
    * @param filter the query filter to apply the the delete operation
    * @param options the options to apply to the delete operation
    * @return a Observable with a single element the DeleteResult or with an com.mongodb.MongoException
    * @since 1.2
    */
  def deleteMany[M:ClassTag:TypeTag](filter: JsObject, options: DeleteOptions): Future[DeleteResult] =
    getCollection[M].deleteMany(filter, options)

  /**
    * Removes all documents from the collection that match the given query filter.  If no documents match, the collection is not modified.
    *
    * @param clientSession the client session with which to associate this operation
    * @param filter the query filter to apply the the delete operation
    * @return a Observable with a single element the DeleteResult or with an com.mongodb.MongoException
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def deleteMany[M:ClassTag:TypeTag](clientSession: ClientSession, filter: JsObject): Future[DeleteResult] =
    getCollection[M].deleteMany(clientSession, filter)

  /**
    * Removes all documents from the collection that match the given query filter.  If no documents match, the collection is not modified.
    *
    * @param clientSession the client session with which to associate this operation
    * @param filter the query filter to apply the the delete operation
    * @param options the options to apply to the delete operation
    * @return a Observable with a single element the DeleteResult or with an com.mongodb.MongoException
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def deleteMany[M:ClassTag:TypeTag](clientSession: ClientSession, filter: JsObject, options: DeleteOptions): Future[DeleteResult] =
    getCollection[M].deleteMany(clientSession, filter, options)
  
  def count[M:ClassTag:TypeTag](): Future[Long] = {
    getCollection[M].count()
  }

  def count[M:ClassTag:TypeTag](filter: JsObject): Future[Long] = {
    getCollection[M].count(filter)
  }

  /**
    * Counts the number of documents in the collection according to the given options.
    *
    * @param filter  the query filter
    * @param options the options describing the count
    * @return a Observable with a single element indicating the number of documents
    */
  def count[M:ClassTag:TypeTag](filter: JsObject, options: CountOptions): Future[Long] =
    getCollection[M].count(filter, options)

  /**
    * Counts the number of documents in the collection.
    *
    * @param clientSession the client session with which to associate this operation
    * @return a Observable with a single element indicating the number of documents
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def count[M:ClassTag:TypeTag](clientSession: ClientSession): Future[Long] =
    getCollection[M].count(clientSession)

  /**
    * Counts the number of documents in the collection according to the given options.
    *
    * @param clientSession the client session with which to associate this operation
    * @param filter the query filter
    * @return a Observable with a single element indicating the number of documents
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def count[M:ClassTag:TypeTag](clientSession: ClientSession, filter: JsObject): Future[Long] =
    getCollection[M].count(clientSession, filter)

  /**
    * Counts the number of documents in the collection according to the given options.
    *
    * @param clientSession the client session with which to associate this operation
    * @param filter  the query filter
    * @param options the options describing the count
    * @return a Observable with a single element indicating the number of documents
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def count[M:ClassTag:TypeTag](clientSession: ClientSession, filter: JsObject, options: CountOptions): Future[Long] =
    getCollection[M].count(clientSession, filter, options)

  /**
    * Gets the distinct values of the specified field name.
    *
    * [[http://docs.mongodb.org/manual/reference/command/distinct/ Distinct]]
    * @param fieldName the field name
    * @tparam M      the target type of the observable.
    * @return a Observable emitting the sequence of distinct values
    */
  def distinct[M:ClassTag:TypeTag](fieldName: String): Future[List[M]] =
    getCollection[M].distinct(fieldName)

  /**
    * Gets the distinct values of the specified field name.
    *
    * [[http://docs.mongodb.org/manual/reference/command/distinct/ Distinct]]
    * @param fieldName the field name
    * @param filter  the query filter
    * @tparam M       the target type of the observable.
    * @return a Observable emitting the sequence of distinct values
    */
  def distinct[M:ClassTag:TypeTag](fieldName: String, filter: JsObject): Future[List[M]] =
    getCollection[M].distinct(fieldName, filter)

  /**
    * Gets the distinct values of the specified field name.
    *
    * [[http://docs.mongodb.org/manual/reference/command/distinct/ Distinct]]
    * @param clientSession the client session with which to associate this operation
    * @param fieldName the field name
    * @tparam M     the target type of the observable.
    * @return a Observable emitting the sequence of distinct values
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def distinct[M:ClassTag:TypeTag](clientSession: ClientSession, fieldName: String): Future[List[M]] =
    getCollection[M].distinct(clientSession, fieldName)

  /**
    * Gets the distinct values of the specified field name.
    *
    * [[http://docs.mongodb.org/manual/reference/command/distinct/ Distinct]]
    * @param clientSession the client session with which to associate this operation
    * @param fieldName the field name
    * @param filter  the query filter
    * @tparam M      the target type of the observable.
    * @return a Observable emitting the sequence of distinct values
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def distinct[M:ClassTag:TypeTag](clientSession: ClientSession, fieldName: String, filter: JsObject): Future[List[M]] =
    getCollection[M].distinct(clientSession, fieldName, filter)

  /**
    * Get the collection according to it's TypeTag.
    * @tparam M
    * @return
    */
  private def getCollection[M:ClassTag:TypeTag]: MongoCollection[M] =
    database.getCollection[M](Mongo.getCollectionName(implicitly[TypeTag[M]]))


  def close(): Unit = {
    client.close()
  }
}
