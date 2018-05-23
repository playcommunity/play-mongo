package cn.playscala.mongo

import java.util

import cn.playscala.mongo.annotations.Entity
import cn.playscala.mongo.client.{ClientSession, FindBuilder}
import cn.playscala.mongo.internal.DefaultHelper.DefaultsTo
import cn.playscala.mongo.internal.AsyncResultHelper._
import com.mongodb.async.SingleResultCallback
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.model._
import com.mongodb.client.result.{DeleteResult, UpdateResult}
import com.mongodb.{MongoNamespace, ReadConcern, ReadPreference, WriteConcern}
import org.bson.BsonDocument
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import com.mongodb.async.client.{AggregateIterable, ChangeStreamIterable, MongoCollection => JMongoCollection}

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.{JsObject, Json}
import cn.playscala.mongo.codecs.Implicits.toBsonDocument
import cn.playscala.mongo.internal.{CodecHelper, ReflectHelper}

import scala.reflect.runtime.universe._

/**
 * The MongoCollection representation.
 *
 * @param wrapped the underlying java MongoCollection
 * @tparam TDocument The type that this collection will encode documents from and decode documents to.
 * @since 1.0
 */
case class MongoCollection[TDocument](val wrapped: JMongoCollection[TDocument]) {

  /**
   * Gets the namespace of this collection.
   *
   * @return the namespace
   */
  lazy val namespace: MongoNamespace = wrapped.getNamespace

  /**
   * Get the default class to cast any documents returned from the database into.
   *
   * @return the default class to cast any documents into
   */
  lazy val documentClass: Class[TDocument] = wrapped.getDocumentClass

  /**
   * Get the codec registry for the MongoDatabase.
   *
   * @return the { @link org.bson.cn.playscala.mongo.codecs.configuration.CodecRegistry}
   */
  lazy val codecRegistry: CodecRegistry = wrapped.getCodecRegistry

  /**
   * Get the read preference for the MongoDatabase.
   *
   * @return the { @link com.mongodb.ReadPreference}
   */
  lazy val readPreference: ReadPreference = wrapped.getReadPreference

  /**
   * Get the write concern for the MongoDatabase.
   *
   * @return the { @link com.mongodb.WriteConcern}
   */
  lazy val writeConcern: WriteConcern = wrapped.getWriteConcern

  /**
   * Get the read concern for the MongoDatabase.
   *
   * @return the [[ReadConcern]]
   * @since 1.1
   */
  lazy val readConcern: ReadConcern = wrapped.getReadConcern

  /**
   * Create a new MongoCollection instance with a different default class to cast any documents returned from the database into..
   *
   * @tparam C   The type that the new collection will encode documents from and decode documents to
   * @return a new MongoCollection instance with the different default class
   */
  def withDocumentClass[C]()(implicit e: C DefaultsTo JsObject, ct: ClassTag[C]): MongoCollection[C] =
    MongoCollection(wrapped.withDocumentClass(ct))

  /**
   * Create a new MongoCollection instance with a different codec registry.
   *
   * @param codecRegistry the new { @link org.bson.cn.playscala.mongo.codecs.configuration.CodecRegistry} for the collection
   * @return a new MongoCollection instance with the different codec registry
   */
  def withCodecRegistry(codecRegistry: CodecRegistry): MongoCollection[TDocument] = MongoCollection(wrapped.withCodecRegistry(codecRegistry))

  /**
   * Create a new MongoCollection instance with a different read preference.
   *
   * @param readPreference the new { @link com.mongodb.ReadPreference} for the collection
   * @return a new MongoCollection instance with the different readPreference
   */
  def withReadPreference(readPreference: ReadPreference): MongoCollection[TDocument] = MongoCollection(wrapped.withReadPreference(readPreference))

  /**
   * Create a new MongoCollection instance with a different write concern.
   *
   * @param writeConcern the new { @link com.mongodb.WriteConcern} for the collection
   * @return a new MongoCollection instance with the different writeConcern
   */
  def withWriteConcern(writeConcern: WriteConcern): MongoCollection[TDocument] = MongoCollection(wrapped.withWriteConcern(writeConcern))

  /**
   * Create a new MongoCollection instance with a different read concern.
   *
   * @param readConcern the new [[ReadConcern]] for the collection
   * @return a new MongoCollection instance with the different ReadConcern
   * @since 1.1
   */
  def withReadConcern(readConcern: ReadConcern): MongoCollection[TDocument] = MongoCollection(wrapped.withReadConcern(readConcern))

  /**
   * Counts the number of documents in the collection.
   *
   * @return a Observable with a single element indicating the number of documents
   */
  def count(): Future[Long] = toFuture[java.lang.Long](wrapped.count(_: SingleResultCallback[java.lang.Long])).map(_.toLong)

  /**
   * Counts the number of documents in the collection according to the given options.
   *
   * @param filter the query filter
   * @return a Observable with a single element indicating the number of documents
   */
  def count(filter: JsObject): Future[Long] = toFuture[java.lang.Long](wrapped.count(filter, _: SingleResultCallback[java.lang.Long])).map(_.toLong)

  /**
   * Counts the number of documents in the collection according to the given options.
   *
   * @param filter  the query filter
   * @param options the options describing the count
   * @return a Observable with a single element indicating the number of documents
   */
  def count(filter: JsObject, options: CountOptions): Future[Long] =
    toFuture[java.lang.Long](wrapped.count(filter, options, _: SingleResultCallback[java.lang.Long])).map(_.toLong)

  /**
   * Counts the number of documents in the collection.
   *
   * @param clientSession the client session with which to associate this operation
   * @return a Observable with a single element indicating the number of documents
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def count(clientSession: ClientSession): Future[Long] =
    toFuture[java.lang.Long](wrapped.count(clientSession, _: SingleResultCallback[java.lang.Long])).map(_.toLong)

  /**
   * Counts the number of documents in the collection according to the given options.
   *
   * @param clientSession the client session with which to associate this operation
   * @param filter the query filter
   * @return a Observable with a single element indicating the number of documents
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def count(clientSession: ClientSession, filter: JsObject): Future[Long] =
    toFuture[java.lang.Long](wrapped.count(clientSession, filter, _: SingleResultCallback[java.lang.Long])).map(_.toLong)

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
  def count(clientSession: ClientSession, filter: JsObject, options: CountOptions): Future[Long] =
    toFuture[java.lang.Long](wrapped.count(clientSession, filter, options, _: SingleResultCallback[java.lang.Long])).map(_.toLong)

  /**
   * Gets the distinct values of the specified field name.
   *
   * [[http://docs.mongodb.org/manual/reference/command/distinct/ Distinct]]
   * @param fieldName the field name
   * @tparam C       the target type of the observable.
   * @return a Observable emitting the sequence of distinct values
   */
  def distinct[C](fieldName: String)(implicit ct: ClassTag[C]): Future[List[C]] =
    toFuture(wrapped.distinct(fieldName, ct))

  /**
   * Gets the distinct values of the specified field name.
   *
   * [[http://docs.mongodb.org/manual/reference/command/distinct/ Distinct]]
   * @param fieldName the field name
   * @param filter  the query filter
   * @tparam C       the target type of the observable.
   * @return a Observable emitting the sequence of distinct values
   */
  def distinct[C](fieldName: String, filter: JsObject)(implicit ct: ClassTag[C]): Future[List[C]] =
    toFuture(wrapped.distinct(fieldName, filter, ct))

  /**
   * Gets the distinct values of the specified field name.
   *
   * [[http://docs.mongodb.org/manual/reference/command/distinct/ Distinct]]
   * @param clientSession the client session with which to associate this operation
   * @param fieldName the field name
   * @tparam C       the target type of the observable.
   * @return a Observable emitting the sequence of distinct values
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def distinct[C](clientSession: ClientSession, fieldName: String)(implicit ct: ClassTag[C]): Future[List[C]] =
    toFuture(wrapped.distinct(clientSession, fieldName, ct))

  /**
   * Gets the distinct values of the specified field name.
   *
   * [[http://docs.mongodb.org/manual/reference/command/distinct/ Distinct]]
   * @param clientSession the client session with which to associate this operation
   * @param fieldName the field name
   * @param filter  the query filter
   * @tparam C       the target type of the observable.
   * @return a Observable emitting the sequence of distinct values
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def distinct[C](clientSession: ClientSession, fieldName: String, filter: JsObject)(implicit ct: ClassTag[C]): Future[List[C]] =
    toFuture(wrapped.distinct(clientSession, fieldName, filter, ct))

  /**
   * Finds all documents in the collection.
   *
   * [[http://docs.mongodb.org/manual/tutorial/query-documents/ Find]]
   *
   * @tparam C   the target document type.
   * @return the FindBuilder
   */
  def find[C]()(implicit e: C DefaultsTo TDocument, ct: ClassTag[C], tt: TypeTag[C]): FindBuilder[C] = {
    FindBuilder(wrapped.find[C](ct), this)
  }

  /**
   * Finds all documents in the collection.
   *
   * [[http://docs.mongodb.org/manual/tutorial/query-documents/ Find]]
   * @param filter the query filter
   * @tparam C    the target document type.
   * @return the future of result list.
   */
  def find[C](filter: JsObject)(implicit e: C DefaultsTo TDocument, ct: ClassTag[C], tt: TypeTag[C]): FindBuilder[C] = {
    new FindBuilder(wrapped.find(filter, ct), this, filter)
  }

  /*
  def fetch[R](field: String)(implicit mClassTag: ClassTag[TDocument], mTypeTag: TypeTag[TDocument], rClassTag: ClassTag[R], rTypeTag: TypeTag[R]): Future[List[(TDocument, List[R])]] = {
    val rFieldSuffix = "___"
    val rField1 = s"${field}${rFieldSuffix}"
    aggregate[BsonDocument](
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
        val model = CodecHelper.decodeBsonDocument(bsonDoc, codecRegistry.get(mClassTag))
        val relateModels = bsonDoc.getArray(rField1).getValues.asScala.toList.map{ bsonVal =>
          CodecHelper.decodeBsonDocument(bsonVal.asInstanceOf[BsonDocument], codecRegistry.get(rClassTag))
        }
        (model, relateModels)
      }
    }
  }
  */

  /**
   * Finds all documents in the collection.
   *
   * [[http://docs.mongodb.org/manual/tutorial/query-documents/ Find]]
   * @param filter the query filter
   * @tparam C    the target document type.
   * @return the future of result list.
   */
  def find[C](filter: JsObject, projection: JsObject)(implicit e: C DefaultsTo TDocument, ct: ClassTag[C], tt: TypeTag[C]): FindBuilder[C] = {
    new FindBuilder(wrapped.find(filter, ct), this, filter, projection)
  }

  /**
   * Finds all documents in the collection.
   *
   * [[http://docs.mongodb.org/manual/tutorial/query-documents/ Find]]
   *
   * @param clientSession the client session with which to associate this operation
   * @tparam C   the target document type of the observable.
   * @return the find Observable
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def find[C](clientSession: ClientSession)(implicit e: C DefaultsTo TDocument, ct: ClassTag[C], tt: TypeTag[C]): FindBuilder[C] = {
    new FindBuilder(wrapped.find[C](clientSession, ct), this, clientSession)
  }

  /**
   * Finds all documents in the collection.
   *
   * [[http://docs.mongodb.org/manual/tutorial/query-documents/ Find]]
   * @param clientSession the client session with which to associate this operation
   * @param filter the query filter
   * @tparam C    the target document type of the observable.
   * @return the find Observable
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def find[C](clientSession: ClientSession, filter: JsObject)(implicit e: C DefaultsTo TDocument, ct: ClassTag[C], tt: TypeTag[C]): FindBuilder[C] = {
    new FindBuilder(wrapped.find(clientSession, filter, ct), this, clientSession, filter)
  }

  /**
   * Finds all documents in the collection.
   *
   * [[http://docs.mongodb.org/manual/tutorial/query-documents/ Find]]
   * @param clientSession the client session with which to associate this operation
   * @param filter the query filter
   * @tparam C    the target document type of the observable.
   * @return the find Observable
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def find[C](clientSession: ClientSession, filter: JsObject, projection: JsObject)(implicit e: C DefaultsTo TDocument, ct: ClassTag[C], tt: TypeTag[C]): FindBuilder[C] = {
    new FindBuilder(wrapped.find(clientSession, filter, ct), this, clientSession, filter, projection)
  }

  /**
   * Aggregates documents according to the specified aggregation pipeline.
   *
   * @param pipeline the aggregate pipeline
   * @return a Observable containing the result of the aggregation operation
   *         [[http://docs.mongodb.org/manual/aggregation/ Aggregation]]
   */
  def aggregate[C](pipeline: Seq[JsObject])(implicit e: C DefaultsTo TDocument, ct: ClassTag[C]): AggregateIterable[C] =
    wrapped.aggregate[C](pipeline.map(toBsonDocument).asJava, ct)

  /**
   * Aggregates documents according to the specified aggregation pipeline.
   *
   * @param clientSession the client session with which to associate this operation
   * @param pipeline the aggregate pipeline
   * @return a Observable containing the result of the aggregation operation
   *         [[http://docs.mongodb.org/manual/aggregation/ Aggregation]]
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def aggregate[C](clientSession: ClientSession, pipeline: Seq[Bson])(implicit e: C DefaultsTo TDocument, ct: ClassTag[C]): AggregateIterable[C] =
    wrapped.aggregate[C](clientSession, pipeline.asJava, ct)

  /**
   * Aggregates documents according to the specified map-reduce function.
   *
   * @param mapFunction    A JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
   * @param reduceFunction A JavaScript function that "reduces" to a single object all the values associated with a particular key.
   * @tparam C            the target document type of the observable.
   * @return a Observable containing the result of the map-reduce operation
   *         [[http://docs.mongodb.org/manual/reference/command/mapReduce/ map-reduce]]
   */
  def mapReduce[C](mapFunction: String, reduceFunction: String)(implicit e: C DefaultsTo TDocument, ct: ClassTag[C]): Future[List[C]] =
    toFuture(wrapped.mapReduce(mapFunction, reduceFunction, ct))

  /**
   * Aggregates documents according to the specified map-reduce function.
   *
   * @param clientSession the client session with which to associate this operation
   * @param mapFunction    A JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
   * @param reduceFunction A JavaScript function that "reduces" to a single object all the values associated with a particular key.
   * @tparam C            the target document type of the observable.
   * @return a Observable containing the result of the map-reduce operation
   *         [[http://docs.mongodb.org/manual/reference/command/mapReduce/ map-reduce]]
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def mapReduce[C](clientSession: ClientSession, mapFunction: String, reduceFunction: String)(implicit e: C DefaultsTo TDocument, ct: ClassTag[C]): Future[List[C]] =
    toFuture(wrapped.mapReduce(clientSession, mapFunction, reduceFunction, ct))

  /**
   * Executes a mix of inserts, updates, replaces, and deletes.
   *
   * @param requests the writes to execute
   * @return a Observable with a single element the BulkWriteResult
   */
  def bulkWrite(requests: Seq[_ <: WriteModel[_ <: TDocument]]): Future[BulkWriteResult] =
    toFuture(wrapped.bulkWrite(
      requests.asJava.asInstanceOf[util.List[_ <: WriteModel[_ <: TDocument]]],
      _: SingleResultCallback[BulkWriteResult]
    ))

  /**
   * Executes a mix of inserts, updates, replaces, and deletes.
   *
   * @param requests the writes to execute
   * @param options  the options to apply to the bulk write operation
   * @return a Observable with a single element the BulkWriteResult
   */
  def bulkWrite(requests: Seq[_ <: WriteModel[_ <: TDocument]], options: BulkWriteOptions): Future[BulkWriteResult] =
    toFuture(wrapped.bulkWrite(
      requests.asJava.asInstanceOf[util.List[_ <: WriteModel[_ <: TDocument]]],
      options,
      _: SingleResultCallback[BulkWriteResult]
    ))

  /**
   * Executes a mix of inserts, updates, replaces, and deletes.
   *
   * @param clientSession the client session with which to associate this operation
   * @param requests the writes to execute
   * @return a Observable with a single element the BulkWriteResult
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def bulkWrite(clientSession: ClientSession, requests: Seq[_ <: WriteModel[_ <: TDocument]]): Future[BulkWriteResult] =
    toFuture(wrapped.bulkWrite(
      clientSession,
      requests.asJava.asInstanceOf[util.List[_ <: WriteModel[_ <: TDocument]]],
      _: SingleResultCallback[BulkWriteResult]
    ))

  /**
   * Executes a mix of inserts, updates, replaces, and deletes.
   *
   * @param clientSession the client session with which to associate this operation
   * @param requests the writes to execute
   * @param options  the options to apply to the bulk write operation
   * @return a Observable with a single element the BulkWriteResult
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def bulkWrite(clientSession: ClientSession, requests: Seq[_ <: WriteModel[_ <: TDocument]], options: BulkWriteOptions): Future[BulkWriteResult] =
    toFuture(wrapped.bulkWrite(
      clientSession,
      requests.asJava.asInstanceOf[util.List[_ <: WriteModel[_ <: TDocument]]],
      options,
      _: SingleResultCallback[BulkWriteResult]
    ))

  /**
   * Inserts the provided document. If the document is missing an identifier, the driver should generate one.
   *
   * @param document the document to insert
   * @return a Observable with a single element indicating when the operation has completed or with either a
   *         com.mongodb.DuplicateKeyException or com.mongodb.MongoException
   */
  def insertOne(document: TDocument): Future[Void] = toFuture(wrapped.insertOne(document, _: SingleResultCallback[Void]))

  /**
   * Inserts the provided document. If the document is missing an identifier, the driver should generate one.
   *
   * @param document the document to insert
   * @param options  the options to apply to the operation
   * @return a Observable with a single element indicating when the operation has completed or with either a
   *         com.mongodb.DuplicateKeyException or com.mongodb.MongoException
   * @since 1.1
   */
  def insertOne(document: TDocument, options: InsertOneOptions): Future[Void] =
    toFuture(wrapped.insertOne(document, options, _: SingleResultCallback[Void]))

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
  def insertOne(clientSession: ClientSession, document: TDocument): Future[Void] =
    toFuture(wrapped.insertOne(clientSession, document, _: SingleResultCallback[Void]))

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
  def insertOne(clientSession: ClientSession, document: TDocument, options: InsertOneOptions): Future[Void] =
    toFuture(wrapped.insertOne(clientSession, document, options, _: SingleResultCallback[Void]))

  /**
   * Inserts a batch of documents. The preferred way to perform bulk inserts is to use the BulkWrite API. However, when talking with a
   * server &lt; 2.6, using this method will be faster due to constraints in the bulk API related to error handling.
   *
   * @param documents the documents to insert
   * @return a Observable with a single element indicating when the operation has completed or with either a
   *         com.mongodb.DuplicateKeyException or com.mongodb.MongoException
   */
  def insertMany(documents: Seq[_ <: TDocument]): Future[Void] =
    toFuture(wrapped.insertMany(documents.asJava, _: SingleResultCallback[Void]))

  /**
   * Inserts a batch of documents. The preferred way to perform bulk inserts is to use the BulkWrite API. However, when talking with a
   * server &lt; 2.6, using this method will be faster due to constraints in the bulk API related to error handling.
   *
   * @param documents the documents to insert
   * @param options   the options to apply to the operation
   * @return a Observable with a single element indicating when the operation has completed or with either a
   *         com.mongodb.DuplicateKeyException or com.mongodb.MongoException
   */
  def insertMany(documents: Seq[_ <: TDocument], options: InsertManyOptions): Future[Void] =
    toFuture(wrapped.insertMany(documents.asJava, options, _: SingleResultCallback[Void]))

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
  def insertMany(clientSession: ClientSession, documents: Seq[_ <: TDocument]): Future[Void] =
    toFuture(wrapped.insertMany(clientSession, documents.asJava, _: SingleResultCallback[Void]))

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
  def insertMany(clientSession: ClientSession, documents: Seq[_ <: TDocument], options: InsertManyOptions): Future[Void] =
    toFuture(wrapped.insertMany(clientSession, documents.asJava, options, _: SingleResultCallback[Void]))

  /**
   * Removes at most one document from the collection that matches the given filter.  If no documents match, the collection is not
   * modified.
   *
   * @param filter the query filter to apply the the delete operation
   * @return a Observable with a single element the DeleteResult or with an com.mongodb.MongoException
   */
  def deleteOne(filter: JsObject): Future[DeleteResult] = toFuture(wrapped.deleteOne(filter, _: SingleResultCallback[DeleteResult]))

  /**
   * Removes at most one document from the collection that matches the given filter.  If no documents match, the collection is not
   * modified.
   *
   * @param filter the query filter to apply the the delete operation
   * @param options the options to apply to the delete operation
   * @return a Observable with a single element the DeleteResult or with an com.mongodb.MongoException
   * @since 1.2
   */
  def deleteOne(filter: JsObject, options: DeleteOptions): Future[DeleteResult] =
    toFuture(wrapped.deleteOne(filter, options, _: SingleResultCallback[DeleteResult]))

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
  def deleteOne(clientSession: ClientSession, filter: JsObject): Future[DeleteResult] =
    toFuture(wrapped.deleteOne(clientSession, filter, _: SingleResultCallback[DeleteResult]))

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
  def deleteOne(clientSession: ClientSession, filter: JsObject, options: DeleteOptions): Future[DeleteResult] =
    toFuture(wrapped.deleteOne(clientSession, filter, options, _: SingleResultCallback[DeleteResult]))

  /**
   * Removes all documents from the collection that match the given query filter.  If no documents match, the collection is not modified.
   *
   * @param filter the query filter to apply the the delete operation
   * @return a Observable with a single element the DeleteResult or with an com.mongodb.MongoException
   */
  def deleteMany(filter: JsObject): Future[DeleteResult] = toFuture(wrapped.deleteMany(filter, _: SingleResultCallback[DeleteResult]))

  /**
   * Removes all documents from the collection that match the given query filter.  If no documents match, the collection is not modified.
   *
   * @param filter the query filter to apply the the delete operation
   * @param options the options to apply to the delete operation
   * @return a Observable with a single element the DeleteResult or with an com.mongodb.MongoException
   * @since 1.2
   */
  def deleteMany(filter: JsObject, options: DeleteOptions): Future[DeleteResult] =
    toFuture(wrapped.deleteMany(filter, options, _: SingleResultCallback[DeleteResult]))

  /**
   * Removes all documents from the collection that match the given query filter.  If no documents match, the collection is not modified.
   *
   * @param clientSession the client session with which to associate this operation
   * @param filter the query filter to apply the the delete operation
   * @return a Observable with a single element the DeleteResult or with an com.mongodb.MongoException
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def deleteMany(clientSession: ClientSession, filter: JsObject): Future[DeleteResult] =
    toFuture(wrapped.deleteMany(clientSession, filter, _: SingleResultCallback[DeleteResult]))

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
  def deleteMany(clientSession: ClientSession, filter: JsObject, options: DeleteOptions): Future[DeleteResult] =
    toFuture(wrapped.deleteMany(clientSession, filter, options, _: SingleResultCallback[DeleteResult]))

  /**
   * Replace a document in the collection according to the specified arguments.
   *
   * [[http://docs.mongodb.org/manual/tutorial/modify-documents/#replace-the-document Replace]]
   * @param filter      the query filter to apply the the replace operation
   * @param replacement the replacement document
   * @return a Observable with a single element the UpdateResult
   */
  def replaceOne(filter: JsObject, replacement: TDocument): Future[UpdateResult] =
    toFuture(wrapped.replaceOne(filter, replacement, _: SingleResultCallback[UpdateResult]))

  /**
   * Replace a document in the collection according to the specified arguments.
   *
   * [[http://docs.mongodb.org/manual/tutorial/modify-documents/#replace-the-document Replace]]
   * @param filter      the query filter to apply the the replace operation
   * @param replacement the replacement document
   * @param options     the options to apply to the replace operation
   * @return a Observable with a single element the UpdateResult
   */
  def replaceOne(filter: JsObject, replacement: TDocument, options: UpdateOptions): Future[UpdateResult] =
    toFuture(wrapped.replaceOne(filter, replacement, options, _: SingleResultCallback[UpdateResult]))

  /**
   * Replace a document in the collection according to the specified arguments.
   *
   * [[http://docs.mongodb.org/manual/tutorial/modify-documents/#replace-the-document Replace]]
   * @param clientSession the client session with which to associate this operation
   * @param filter      the query filter to apply the the replace operation
   * @param replacement the replacement document
   * @return a Observable with a single element the UpdateResult
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def replaceOne(clientSession: ClientSession, filter: JsObject, replacement: TDocument): Future[UpdateResult] =
    toFuture(wrapped.replaceOne(clientSession, filter, replacement, _: SingleResultCallback[UpdateResult]))

  /**
   * Replace a document in the collection according to the specified arguments.
   *
   * [[http://docs.mongodb.org/manual/tutorial/modify-documents/#replace-the-document Replace]]
   * @param clientSession the client session with which to associate this operation
   * @param filter      the query filter to apply the the replace operation
   * @param replacement the replacement document
   * @param options     the options to apply to the replace operation
   * @return a Observable with a single element the UpdateResult
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def replaceOne(clientSession: ClientSession, filter: JsObject, replacement: TDocument, options: UpdateOptions): Future[UpdateResult] =
    toFuture(wrapped.replaceOne(clientSession, filter, replacement, options, _: SingleResultCallback[UpdateResult]))

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
  def updateOne(filter: JsObject, update: JsObject): Future[UpdateResult] =
    toFuture(wrapped.updateOne(filter, update, _: SingleResultCallback[UpdateResult]))

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
  def updateOne(filter: JsObject, update: JsObject, options: UpdateOptions): Future[UpdateResult] =
    toFuture(wrapped.updateOne(filter, update, options, _: SingleResultCallback[UpdateResult]))

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
  def updateOne(clientSession: ClientSession, filter: JsObject, update: JsObject): Future[UpdateResult] =
    toFuture(wrapped.updateOne(clientSession, filter, update, _: SingleResultCallback[UpdateResult]))

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
  def updateOne(clientSession: ClientSession, filter: JsObject, update: JsObject, options: UpdateOptions): Future[UpdateResult] =
    toFuture(wrapped.updateOne(clientSession, filter, update, options, _: SingleResultCallback[UpdateResult]))

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
  def updateMany(filter: JsObject, update: JsObject): Future[UpdateResult] =
    toFuture(wrapped.updateMany(filter, update, _: SingleResultCallback[UpdateResult]))

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
  def updateMany(filter: JsObject, update: JsObject, options: UpdateOptions): Future[UpdateResult] =
    toFuture(wrapped.updateMany(filter, update, options, _: SingleResultCallback[UpdateResult]))

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
  def updateMany(clientSession: ClientSession, filter: JsObject, update: JsObject): Future[UpdateResult] =
    toFuture(wrapped.updateMany(clientSession, filter, update, _: SingleResultCallback[UpdateResult]))

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
  def updateMany(clientSession: ClientSession, filter: JsObject, update: JsObject, options: UpdateOptions): Future[UpdateResult] =
    toFuture(wrapped.updateMany(clientSession, filter, update, options, _: SingleResultCallback[UpdateResult]))

  /**
   * Atomically find a document and remove it.
   *
   * @param filter  the query filter to find the document with
   * @return a Observable with a single element the document that was removed.  If no documents matched the query filter, then null will be
   *         returned
   */
  def findOneAndDelete(filter: JsObject): Future[TDocument] = toFuture(wrapped.findOneAndDelete(filter, _: SingleResultCallback[TDocument]))

  /**
   * Atomically find a document and remove it.
   *
   * @param filter  the query filter to find the document with
   * @param options the options to apply to the operation
   * @return a Observable with a single element the document that was removed.  If no documents matched the query filter, then null will be
   *         returned
   */
  def findOneAndDelete(filter: JsObject, options: FindOneAndDeleteOptions): Future[TDocument] =
    toFuture(wrapped.findOneAndDelete(filter, options, _: SingleResultCallback[TDocument]))

  /**
   * Atomically find a document and remove it.
   *
   * @param clientSession the client session with which to associate this operation
   * @param filter  the query filter to find the document with
   * @return a Observable with a single element the document that was removed.  If no documents matched the query filter, then null will be
   *         returned
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def findOneAndDelete(clientSession: ClientSession, filter: JsObject): Future[TDocument] =
    toFuture(wrapped.findOneAndDelete(clientSession, filter, _: SingleResultCallback[TDocument]))

  /**
   * Atomically find a document and remove it.
   *
   * @param clientSession the client session with which to associate this operation
   * @param filter  the query filter to find the document with
   * @param options the options to apply to the operation
   * @return a Observable with a single element the document that was removed.  If no documents matched the query filter, then null will be
   *         returned
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def findOneAndDelete(clientSession: ClientSession, filter: JsObject, options: FindOneAndDeleteOptions): Future[TDocument] =
    toFuture(wrapped.findOneAndDelete(clientSession, filter, options, _: SingleResultCallback[TDocument]))

  /**
   * Atomically find a document and replace it.
   *
   * @param filter      the query filter to apply the the replace operation
   * @param replacement the replacement document
   * @return a Observable with a single element the document that was replaced.  Depending on the value of the `returnOriginal`
   *         property, this will either be the document as it was before the update or as it is after the update.  If no documents matched the
   *         query filter, then null will be returned
   */
  def findOneAndReplace(filter: JsObject, replacement: TDocument): Future[TDocument] =
    toFuture(wrapped.findOneAndReplace(filter, replacement, _: SingleResultCallback[TDocument]))

  /**
   * Atomically find a document and replace it.
   *
   * @param filter      the query filter to apply the the replace operation
   * @param replacement the replacement document
   * @param options     the options to apply to the operation
   * @return a Observable with a single element the document that was replaced.  Depending on the value of the `returnOriginal`
   *         property, this will either be the document as it was before the update or as it is after the update.  If no documents matched the
   *         query filter, then null will be returned
   */
  def findOneAndReplace(filter: JsObject, replacement: TDocument, options: FindOneAndReplaceOptions): Future[TDocument] =
    toFuture(wrapped.findOneAndReplace(filter, replacement, options, _: SingleResultCallback[TDocument]))

  /**
   * Atomically find a document and replace it.
   *
   * @param clientSession the client session with which to associate this operation
   * @param filter      the query filter to apply the the replace operation
   * @param replacement the replacement document
   * @return a Observable with a single element the document that was replaced.  Depending on the value of the `returnOriginal`
   *         property, this will either be the document as it was before the update or as it is after the update.  If no documents matched the
   *         query filter, then null will be returned
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def findOneAndReplace(clientSession: ClientSession, filter: JsObject, replacement: TDocument): Future[TDocument] =
    toFuture(wrapped.findOneAndReplace(clientSession, filter, replacement, _: SingleResultCallback[TDocument]))

  /**
   * Atomically find a document and replace it.
   *
   * @param clientSession the client session with which to associate this operation
   * @param filter      the query filter to apply the the replace operation
   * @param replacement the replacement document
   * @param options     the options to apply to the operation
   * @return a Observable with a single element the document that was replaced.  Depending on the value of the `returnOriginal`
   *         property, this will either be the document as it was before the update or as it is after the update.  If no documents matched the
   *         query filter, then null will be returned
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def findOneAndReplace(clientSession: ClientSession, filter: JsObject, replacement: TDocument, options: FindOneAndReplaceOptions): Future[TDocument] =
    toFuture(wrapped.findOneAndReplace(clientSession, filter, replacement, options, _: SingleResultCallback[TDocument]))

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
  def findOneAndUpdate(filter: JsObject, update: JsObject): Future[TDocument] =
    toFuture(wrapped.findOneAndUpdate(filter, update, _: SingleResultCallback[TDocument]))

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
  def findOneAndUpdate(filter: JsObject, update: JsObject, options: FindOneAndUpdateOptions): Future[TDocument] =
    toFuture(wrapped.findOneAndUpdate(filter, update, options, _: SingleResultCallback[TDocument]))

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
  def findOneAndUpdate(clientSession: ClientSession, filter: JsObject, update: JsObject): Future[TDocument] =
    toFuture(wrapped.findOneAndUpdate(clientSession, filter, update, _: SingleResultCallback[TDocument]))

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
  def findOneAndUpdate(clientSession: ClientSession, filter: JsObject, update: JsObject, options: FindOneAndUpdateOptions): Future[TDocument] =
    toFuture(wrapped.findOneAndUpdate(clientSession, filter, update, options, _: SingleResultCallback[TDocument]))

  /**
   * Drops this collection from the Database.
   *
   * @return a Observable with a single element indicating when the operation has completed
   *         [[http://docs.mongodb.org/manual/reference/command/drop/ Drop Collection]]
   */
  def drop(): Future[Void] = toFuture(wrapped.drop(_: SingleResultCallback[Void]))

  /**
   * Drops this collection from the Database.
   *
   * @param clientSession the client session with which to associate this operation
   * @return a Observable with a single element indicating when the operation has completed
   *         [[http://docs.mongodb.org/manual/reference/command/drop/ Drop Collection]]
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def drop(clientSession: ClientSession): Future[Void] = toFuture(wrapped.drop(clientSession, _: SingleResultCallback[Void]))

  /**
   * [[http://docs.mongodb.org/manual/reference/command/createIndexes Create Index]]
   * @param key     an object describing the index key(s), which may not be null. This can be of any type for which a `Codec` is
   *                registered
   * @return a Observable with a single element indicating when the operation has completed
   */
  def createIndex(key: JsObject): Future[String] =
    toFuture(wrapped.createIndex(key, _: SingleResultCallback[String]))

  /**
   * [[http://docs.mongodb.org/manual/reference/command/createIndexes Create Index]]
   * @param key     an object describing the index key(s), which may not be null. This can be of any type for which a `Codec` is
   *                registered
   * @param options the options for the index
   * @return a Observable with a single element indicating when the operation has completed
   */
  def createIndex(key: JsObject, options: IndexOptions): Future[String] =
    toFuture(wrapped.createIndex(key, options, _: SingleResultCallback[String]))

  /**
   * [[http://docs.mongodb.org/manual/reference/command/createIndexes Create Index]]
   * @param clientSession the client session with which to associate this operation
   * @param key     an object describing the index key(s), which may not be null. This can be of any type for which a `Codec` is
   *                registered
   * @return a Observable with a single element indicating when the operation has completed
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def createIndex(clientSession: ClientSession, key: JsObject): Future[String] =
    toFuture(wrapped.createIndex(clientSession, key, _: SingleResultCallback[String]))

  /**
   * [[http://docs.mongodb.org/manual/reference/command/createIndexes Create Index]]
   * @param clientSession the client session with which to associate this operation
   * @param key     an object describing the index key(s), which may not be null. This can be of any type for which a `Codec` is
   *                registered
   * @param options the options for the index
   * @return a Observable with a single element indicating when the operation has completed
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def createIndex(clientSession: ClientSession, key: JsObject, options: IndexOptions): Future[String] =
    toFuture(wrapped.createIndex(clientSession, key, options, _: SingleResultCallback[String]))

  /**
   * Create multiple indexes.
   *
   * [[http://docs.mongodb.org/manual/reference/command/createIndexes Create Index]]
   * @param models the list of indexes to create
   * @return a Observable with the names of the indexes
   */
  def createIndexes(models: Seq[IndexModel]): Future[String] =
    toFuture[util.List[String]](wrapped.createIndexes(models.asJava, _: SingleResultCallback[util.List[String]])).map(_.asScala.mkString(","))

  /**
   * Create multiple indexes.
   *
   * [[http://docs.mongodb.org/manual/reference/command/createIndexes Create Index]]
   * @param models the list of indexes to create
   * @param createIndexOptions options to use when creating indexes
   * @return a Observable with the names of the indexes
   * @since 2.2
   */
  def createIndexes(models: Seq[IndexModel], createIndexOptions: CreateIndexOptions): Future[String] =
    toFuture[util.List[String]](wrapped.createIndexes(models.asJava, createIndexOptions, _: SingleResultCallback[util.List[String]])).map(_.asScala.mkString(","))

  /**
   * Create multiple indexes.
   *
   * [[http://docs.mongodb.org/manual/reference/command/createIndexes Create Index]]
   * @param clientSession the client session with which to associate this operation
   * @param models the list of indexes to create
   * @return a Observable with the names of the indexes
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def createIndexes(clientSession: ClientSession, models: Seq[IndexModel]): Future[String] =
    toFuture[util.List[String]](wrapped.createIndexes(clientSession, models.asJava, _: SingleResultCallback[util.List[String]])).map(_.asScala.mkString(","))

  /**
   * Create multiple indexes.
   *
   * [[http://docs.mongodb.org/manual/reference/command/createIndexes Create Index]]
   * @param clientSession the client session with which to associate this operation
   * @param models the list of indexes to create
   * @param createIndexOptions options to use when creating indexes
   * @return a Observable with the names of the indexes
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def createIndexes(clientSession: ClientSession, models: Seq[IndexModel], createIndexOptions: CreateIndexOptions): Future[String] =
    toFuture[util.List[String]](wrapped.createIndexes(clientSession, models.asJava, createIndexOptions, _: SingleResultCallback[util.List[String]])).map(_.asScala.mkString(","))

  /**
   * Get all the indexes in this collection.
   *
   * [[http://docs.mongodb.org/manual/reference/command/listIndexes/ listIndexes]]
   * @tparam C   the target document type of the observable.
   * @return the fluent list indexes interface
   */
  def listIndexes[C]()(implicit e: C DefaultsTo JsObject, ct: ClassTag[C]): Future[List[C]] =
    toFuture(wrapped.listIndexes(ct))

  /**
   * Get all the indexes in this collection.
   *
   * [[http://docs.mongodb.org/manual/reference/command/listIndexes/ listIndexes]]
   * @param clientSession the client session with which to associate this operation
   * @tparam C   the target document type of the observable.
   * @return the fluent list indexes interface
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def listIndexes[C](clientSession: ClientSession)(implicit e: C DefaultsTo JsObject, ct: ClassTag[C]): Future[List[C]] =
    toFuture(wrapped.listIndexes(clientSession, ct))

  /**
   * Drops the given index.
   *
   * [[http://docs.mongodb.org/manual/reference/command/dropIndexes/ Drop Indexes]]
   * @param indexName the name of the index to remove
   * @return a Observable with a single element indicating when the operation has completed
   */
  def dropIndex(indexName: String): Future[Void] = toFuture(wrapped.dropIndex(indexName, _: SingleResultCallback[Void]))

  /**
   * Drops the given index.
   *
   * [[http://docs.mongodb.org/manual/reference/command/dropIndexes/ Drop Indexes]]
   * @param indexName the name of the index to remove
   * @param dropIndexOptions options to use when dropping indexes
   * @return a Observable with a single element indicating when the operation has completed
   * @since 2.2
   */
  def dropIndex(indexName: String, dropIndexOptions: DropIndexOptions): Future[Void] =
    toFuture(wrapped.dropIndex(indexName, dropIndexOptions, _: SingleResultCallback[Void]))

  /**
   * Drops the index given the keys used to create it.
   *
   * @param keys the keys of the index to remove
   * @return a Observable with a single element indicating when the operation has completed
   */
  def dropIndex(keys: JsObject): Future[Void] = toFuture(wrapped.dropIndex(keys, _: SingleResultCallback[Void]))

  /**
   * Drops the index given the keys used to create it.
   *
   * @param keys the keys of the index to remove
   * @param dropIndexOptions options to use when dropping indexes
   * @return a Observable with a single element indicating when the operation has completed
   * @since 2.2
   */
  def dropIndex(keys: JsObject, dropIndexOptions: DropIndexOptions): Future[Void] =
    toFuture(wrapped.dropIndex(keys, dropIndexOptions, _: SingleResultCallback[Void]))

  /**
   * Drops the given index.
   *
   * [[http://docs.mongodb.org/manual/reference/command/dropIndexes/ Drop Indexes]]
   * @param clientSession the client session with which to associate this operation
   * @param indexName the name of the index to remove
   * @return a Observable with a single element indicating when the operation has completed
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def dropIndex(clientSession: ClientSession, indexName: String): Future[Void] =
    toFuture(wrapped.dropIndex(clientSession, indexName, _: SingleResultCallback[Void]))

  /**
   * Drops the given index.
   *
   * [[http://docs.mongodb.org/manual/reference/command/dropIndexes/ Drop Indexes]]
   * @param clientSession the client session with which to associate this operation
   * @param indexName the name of the index to remove
   * @param dropIndexOptions options to use when dropping indexes
   * @return a Observable with a single element indicating when the operation has completed
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def dropIndex(clientSession: ClientSession, indexName: String, dropIndexOptions: DropIndexOptions): Future[Void] =
    toFuture(wrapped.dropIndex(clientSession, indexName, dropIndexOptions, _: SingleResultCallback[Void]))

  /**
   * Drops the index given the keys used to create it.
   *
   * @param clientSession the client session with which to associate this operation
   * @param keys the keys of the index to remove
   * @return a Observable with a single element indicating when the operation has completed
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def dropIndex(clientSession: ClientSession, keys: JsObject): Future[Void] =
    toFuture(wrapped.dropIndex(clientSession, keys, _: SingleResultCallback[Void]))

  /**
   * Drops the index given the keys used to create it.
   *
   * @param clientSession the client session with which to associate this operation
   * @param keys the keys of the index to remove
   * @param dropIndexOptions options to use when dropping indexes
   * @return a Observable with a single element indicating when the operation has completed
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def dropIndex(clientSession: ClientSession, keys: JsObject, dropIndexOptions: DropIndexOptions): Future[Void] =
    toFuture(wrapped.dropIndex(clientSession, keys, dropIndexOptions, _: SingleResultCallback[Void]))

  /**
   * Drop all the indexes on this collection, except for the default on _id.
   *
   * [[http://docs.mongodb.org/manual/reference/command/dropIndexes/ Drop Indexes]]
   * @return a Observable with a single element indicating when the operation has completed
   */
  def dropIndexes(): Future[Void] =
    toFuture(wrapped.dropIndexes(_: SingleResultCallback[Void]))

  /**
   * Drop all the indexes on this collection, except for the default on _id.
   *
   * [[http://docs.mongodb.org/manual/reference/command/dropIndexes/ Drop Indexes]]
   * @param dropIndexOptions options to use when dropping indexes
   * @return a Observable with a single element indicating when the operation has completed
   * @since 2.2
   */
  def dropIndexes(dropIndexOptions: DropIndexOptions): Future[Void] =
    toFuture(wrapped.dropIndexes(dropIndexOptions, _: SingleResultCallback[Void]))

  /**
   * Drop all the indexes on this collection, except for the default on _id.
   *
   * [[http://docs.mongodb.org/manual/reference/command/dropIndexes/ Drop Indexes]]
   * @param clientSession the client session with which to associate this operation
   * @return a Observable with a single element indicating when the operation has completed
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def dropIndexes(clientSession: ClientSession): Future[Void] =
    toFuture(wrapped.dropIndexes(clientSession, _: SingleResultCallback[Void]))

  /**
   * Drop all the indexes on this collection, except for the default on _id.
   *
   * [[http://docs.mongodb.org/manual/reference/command/dropIndexes/ Drop Indexes]]
   * @param clientSession the client session with which to associate this operation
   * @param dropIndexOptions options to use when dropping indexes
   * @return a Observable with a single element indicating when the operation has completed
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def dropIndexes(clientSession: ClientSession, dropIndexOptions: DropIndexOptions): Future[Void] =
    toFuture(wrapped.dropIndexes(clientSession, dropIndexOptions, _: SingleResultCallback[Void]))

  /**
   * Rename the collection with oldCollectionName to the newCollectionName.
   *
   * [[http://docs.mongodb.org/manual/reference/commands/renameCollection Rename collection]]
   * @param newCollectionNamespace the name the collection will be renamed to
   * @return a Observable with a single element indicating when the operation has completed
   */
  def renameCollection(newCollectionNamespace: MongoNamespace): Future[Void] =
    toFuture(wrapped.renameCollection(newCollectionNamespace, _: SingleResultCallback[Void]))

  /**
   * Rename the collection with oldCollectionName to the newCollectionName.
   *
   * [[http://docs.mongodb.org/manual/reference/commands/renameCollection Rename collection]]
   * @param newCollectionNamespace the name the collection will be renamed to
   * @param options                the options for renaming a collection
   * @return a Observable with a single element indicating when the operation has completed
   */
  def renameCollection(newCollectionNamespace: MongoNamespace, options: RenameCollectionOptions): Future[Void] =
    toFuture(wrapped.renameCollection(newCollectionNamespace, options, _: SingleResultCallback[Void]))

  /**
   * Rename the collection with oldCollectionName to the newCollectionName.
   *
   * [[http://docs.mongodb.org/manual/reference/commands/renameCollection Rename collection]]
   * @param clientSession the client session with which to associate this operation
   * @param newCollectionNamespace the name the collection will be renamed to
   * @return a Observable with a single element indicating when the operation has completed
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def renameCollection(clientSession: ClientSession, newCollectionNamespace: MongoNamespace): Future[Void] =
    toFuture(wrapped.renameCollection(clientSession, newCollectionNamespace, _: SingleResultCallback[Void]))

  /**
   * Rename the collection with oldCollectionName to the newCollectionName.
   *
   * [[http://docs.mongodb.org/manual/reference/commands/renameCollection Rename collection]]
   * @param clientSession the client session with which to associate this operation
   * @param newCollectionNamespace the name the collection will be renamed to
   * @param options                the options for renaming a collection
   * @return a Observable with a single element indicating when the operation has completed
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def renameCollection(clientSession: ClientSession, newCollectionNamespace: MongoNamespace,
                       options: RenameCollectionOptions): Future[Void] =
    toFuture(wrapped.renameCollection(clientSession, newCollectionNamespace, options, _: SingleResultCallback[Void]))

  /**
   * Creates a change stream for this collection.
   *
   * @tparam C   the target document type of the observable.
   * @return the change stream
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def watch[C]()(implicit e: C DefaultsTo TDocument, ct: ClassTag[C]): ChangeStream[C] = ChangeStream(wrapped.watch(ct))

  /**
   * Creates a change stream for this collection.
   *
   * @param pipeline the aggregation pipeline to apply to the change stream
   * @tparam C   the target document type of the observable.
   * @return the change stream observable
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def watch[C](pipeline: Seq[Bson])(implicit e: C DefaultsTo TDocument, ct: ClassTag[C]): ChangeStreamIterable[C] =
    wrapped.watch(pipeline.asJava, ct)

  /**
   * Creates a change stream for this collection.
   *
   * @param clientSession the client session with which to associate this operation
   * @tparam C   the target document type of the observable.
   * @return the change stream observable
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def watch[C](clientSession: ClientSession)(implicit e: C DefaultsTo TDocument, ct: ClassTag[C]): ChangeStreamIterable[C] =
    wrapped.watch(clientSession, ct)

  /**
   * Creates a change stream for this collection.
   *
   * @param clientSession the client session with which to associate this operation
   * @param pipeline the aggregation pipeline to apply to the change stream
   * @tparam C   the target document type of the observable.
   * @return the change stream observable
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def watch[C](clientSession: ClientSession, pipeline: Seq[Bson])(implicit e: C DefaultsTo TDocument, ct: ClassTag[C]): ChangeStreamIterable[C] =
    wrapped.watch(clientSession, pipeline.asJava, ct)

}
