package cn.playscala.mongo

import cn.playscala.mongo.{MongoClient, MongoConfig, MongoDatabase}
import cn.playscala.mongo.annotations.Entity
import cn.playscala.mongo.client.{ClientSession, FindBuilder}
import cn.playscala.mongo.codecs.IterableCodecProvider
import cn.playscala.mongo.codecs.json.JsonCodecProvider
import cn.playscala.mongo.codecs.macrocodecs.ModelRegistryMacro
import cn.playscala.mongo.codecs.common.{BigDecimalCodec, JOffsetDateTimeCodec}
import cn.playscala.mongo.gridfs.GridFSBucket
import cn.playscala.mongo.internal.AsyncResultHelper.toFuture
import cn.playscala.mongo.internal.DefaultHelper.DefaultsTo
import cn.playscala.mongo.internal.exception.CollectionNameNotFound
import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.MongoClients
import com.mongodb.client.model._
import com.mongodb.client.result.{DeleteResult, UpdateResult}
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromProviders, fromRegistries}
import play.api.libs.json.{JsObject, Json, Writes}

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
    fromCodecs(new BigDecimalCodec),
    fromProviders(IterableCodecProvider())
  )

  def addCodecRegistry(registry: CodecRegistry): Mongo.type = {
    codecRegistry = fromRegistries(codecRegistry, registry)
    this
  }

  def getCollectionName(typeTag: TypeTag[_]): Option[String] = {
    if (typeTag.tpe =:= typeOf[Nothing]) {
      None
    } else {
      val fullClassName = typeTag.tpe.typeSymbol.fullName

      if (!isDevMode && collectionNameMap.contains(fullClassName)) {
        Some(collectionNameMap(fullClassName))
      } else {
        val parsedName =
          typeTag.tpe.typeSymbol.annotations.find(_.tree.tpe =:= typeOf[Entity]).map{ annotation =>
            val Literal(Constant(collectionName: String)) :: Nil = annotation.tree.children.tail
            collectionName
          }.getOrElse(typeTag.tpe.typeSymbol.name.toString)

        collectionNameMap += (fullClassName -> parsedName)
        Some(parsedName)
      }
    }
  }

  @compileTimeOnly("Find case classes utilises Macros and must be run at compile time.")
  def setModelsPackage(modelsPackage: String): Mongo.type = macro ModelRegistryMacro.modelsRegistryImpl
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

  def findById[M, ID:Writes](id: ID)(implicit ct: ClassTag[M], tt: TypeTag[M]): Future[Option[M]] = find[M](Json.obj("_id" -> Json.toJson(id)(implicitly[Writes[ID]]))).first

  def findById[M](id: String)(implicit ct: ClassTag[M], tt: TypeTag[M]): Future[Option[M]] = find[M](Json.obj("_id" -> id)).first

  def findById[M](id: Long)(implicit ct: ClassTag[M], tt: TypeTag[M]): Future[Option[M]] = find[M](Json.obj("_id" -> id)).first

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
    * Atomically find a document and update it.
    *
    * @param filter  a document describing the query filter, which may not be null. This can be of any type for which a `Codec` is
    *                registered
    * @param update  a document describing the update, which may not be null. The update to apply must include only update operators. This
    *                can be of any type for which a `Codec` is registered
    * @return a Observable with a single element the document that was updated.  Depending on the value of the `returnOriginal`
    *         property, this will either be the document as it was before the update or as it is after the update.  If no documents matched the
    *         query filter, then null will be returned
    */
  def findOneAndUpdate[M:ClassTag:TypeTag](filter: JsObject, update: JsObject): Future[Option[M]] =
    getCollection[M].findOneAndUpdate(filter, update)

  /**
    * Atomically find a document and update it.
    *
    * @param filter  a document describing the query filter, which may not be null. This can be of any type for which a `Codec` is
    *                registered
    * @param update  a document describing the update, which may not be null. The update to apply must include only update operators. This
    *                can be of any type for which a `Codec` is registered
    * @param options the options to apply to the operation
    * @return a Observable with a single element the document that was updated.  Depending on the value of the `returnOriginal`
    *         property, this will either be the document as it was before the update or as it is after the update.  If no documents matched the
    *         query filter, then null will be returned
    */
  def findOneAndUpdate[M:ClassTag:TypeTag](filter: JsObject, update: JsObject, options: FindOneAndUpdateOptions): Future[Option[M]] =
    getCollection[M].findOneAndUpdate(filter, update, options)

  /**
    * Atomically find a document and update it.
    *
    * @param clientSession the client session with which to associate this operation
    * @param filter  a document describing the query filter, which may not be null. This can be of any type for which a `Codec` is
    *                registered
    * @param update  a document describing the update, which may not be null. The update to apply must include only update operators. This
    *                can be of any type for which a `Codec` is registered
    * @return a Observable with a single element the document that was updated.  Depending on the value of the `returnOriginal`
    *         property, this will either be the document as it was before the update or as it is after the update.  If no documents matched the
    *         query filter, then null will be returned
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def findOneAndUpdate[M:ClassTag:TypeTag](clientSession: ClientSession, filter: JsObject, update: JsObject): Future[Option[M]] =
    getCollection[M].findOneAndUpdate(clientSession, filter, update)

  /**
    * Atomically find a document and update it.
    *
    * @param clientSession the client session with which to associate this operation
    * @param filter  a document describing the query filter, which may not be null. This can be of any type for which a `Codec` is
    *                registered
    * @param update  a document describing the update, which may not be null. The update to apply must include only update operators. This
    *                can be of any type for which a `Codec` is registered
    * @param options the options to apply to the operation
    * @return a Observable with a single element the document that was updated.  Depending on the value of the `returnOriginal`
    *         property, this will either be the document as it was before the update or as it is after the update.  If no documents matched the
    *         query filter, then null will be returned
    * @since 2.2
    * @note Requires MongoDB 3.6 or greater
    */
  def findOneAndUpdate[M:ClassTag:TypeTag](clientSession: ClientSession, filter: JsObject, update: JsObject, options: FindOneAndUpdateOptions): Future[Option[M]] =
    getCollection[M].findOneAndUpdate(clientSession, filter, update, options)

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

  def updateById[M:ClassTag:TypeTag](_id: String, update: JsObject): Future[UpdateResult] =
    getCollection[M].updateById(_id, update)

  def updateById[M:ClassTag:TypeTag](_id: Long, update: JsObject): Future[UpdateResult] =
    getCollection[M].updateById(_id, update)

  def updateOne[M:ClassTag:TypeTag](filter: JsObject, update: JsObject, upsert: Boolean): Future[UpdateResult] =
    getCollection[M].updateOne(filter, update, new UpdateOptions().upsert(upsert))

  def updateById[M:ClassTag:TypeTag](_id: String, update: JsObject, upsert: Boolean): Future[UpdateResult] =
    getCollection[M].updateById(_id, update, upsert)

  def updateById[M:ClassTag:TypeTag](_id: Long, update: JsObject, upsert: Boolean): Future[UpdateResult] =
    getCollection[M].updateById(_id, update, upsert)


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

  def updateById[M:ClassTag:TypeTag](_id: String, update: JsObject, options: UpdateOptions): Future[UpdateResult] =
    getCollection[M].updateById(_id, update, options)

  def updateById[M:ClassTag:TypeTag](_id: Long, update: JsObject, options: UpdateOptions): Future[UpdateResult] =
    getCollection[M].updateById(_id, update, options)

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
  def updateOne[M:ClassTag:TypeTag](filter: JsObject, update: JsObject, clientSession: ClientSession): Future[UpdateResult] =
    getCollection[M].updateOne(filter, update, clientSession)

  def updateById[M:ClassTag:TypeTag](_id: String, update: JsObject, clientSession: ClientSession): Future[UpdateResult] =
    getCollection[M].updateById(_id, update, clientSession)

  def updateById[M:ClassTag:TypeTag](_id: Long, update: JsObject, clientSession: ClientSession): Future[UpdateResult] =
    getCollection[M].updateById(_id, update, clientSession)

  def updateOne[M:ClassTag:TypeTag](filter: JsObject, update: JsObject, upsert: Boolean, clientSession: ClientSession): Future[UpdateResult] =
    getCollection[M].updateOne(filter, update, new UpdateOptions().upsert(upsert), clientSession)

  def updateById[M:ClassTag:TypeTag](_id: String, update: JsObject, upsert: Boolean, clientSession: ClientSession): Future[UpdateResult] =
    getCollection[M].updateById(_id, update, upsert, clientSession)

  def updateById[M:ClassTag:TypeTag](_id: Long, update: JsObject, upsert: Boolean, clientSession: ClientSession): Future[UpdateResult] =
    getCollection[M].updateById(_id, update, upsert, clientSession)


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
  def updateOne[M:ClassTag:TypeTag](filter: JsObject, update: JsObject, options: UpdateOptions, clientSession: ClientSession): Future[UpdateResult] =
    getCollection[M].updateOne(filter, update, options, clientSession)

  def updateById[M:ClassTag:TypeTag](_id: String, update: JsObject, options: UpdateOptions, clientSession: ClientSession): Future[UpdateResult] =
    getCollection[M].updateById(_id, update, options, clientSession)

  def updateById[M:ClassTag:TypeTag](_id: Long, update: JsObject, options: UpdateOptions, clientSession: ClientSession): Future[UpdateResult] =
    getCollection[M].updateById(_id, update, options, clientSession)

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

  def updateMany[M:ClassTag:TypeTag](filter: JsObject, update: JsObject, upsert: Boolean): Future[UpdateResult] =
    getCollection[M].updateMany(filter, update, new UpdateOptions().upsert(upsert))

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
  def updateMany[M:ClassTag:TypeTag](clientSession: ClientSession, filter: JsObject, update: JsObject, upsert: Boolean): Future[UpdateResult] =
    getCollection[M].updateMany(clientSession, filter, update, new UpdateOptions().upsert(upsert))

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

  def deleteById[M:ClassTag:TypeTag](id: String): Future[DeleteResult] =
    getCollection[M].deleteOne(Json.obj("_id" -> id))

  def deleteById[M:ClassTag:TypeTag](id: Long): Future[DeleteResult] =
    getCollection[M].deleteOne(Json.obj("_id" -> id))
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
    * @return MongoCollection[M]
    */
  def collection[M:ClassTag:TypeTag]: MongoCollection[JsObject] = {
    Mongo.getCollectionName(implicitly[TypeTag[M]]) match {
      case Some(colName) => database.getCollection[JsObject](colName)
      case None => throw CollectionNameNotFound("Fail to parse the collection name from parameter types of the methods in Mongo.")
    }
  }

  private def getCollection[M:ClassTag:TypeTag]: MongoCollection[M] = {
    Mongo.getCollectionName(implicitly[TypeTag[M]]) match {
      case Some(colName) => database.getCollection[M](colName)
      case None => throw CollectionNameNotFound("Fail to parse the collection name from parameter types of the methods in Mongo.")
    }
  }

  /**
    * Get the collection according to it's name.
    * @param collectionName
    * @return MongoCollection[JsObject]
    */
  def collection(collectionName: String): MongoCollection[JsObject] =
    database.getCollection[JsObject](collectionName)

  def close(): Unit = {
    client.close()
  }
}
