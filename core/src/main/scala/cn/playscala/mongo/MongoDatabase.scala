package cn.playscala.mongo

import cn.playscala.mongo.internal.AsyncResultHelper._
import cn.playscala.mongo.internal.DefaultHelper.DefaultsTo
import com.mongodb.async.SingleResultCallback
import com.mongodb.client.model.{CreateCollectionOptions, CreateViewOptions}
import com.mongodb.session.ClientSession
import com.mongodb.{ReadConcern, ReadPreference, WriteConcern}
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import com.mongodb.async.client.{MongoDatabase => JMongoDatabase}
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.JsObject

/**
 * The MongoDatabase representation.
 *
 * @param wrapped the underlying java MongoDatabase
 * @since 1.0
 */
case class MongoDatabase(val wrapped: JMongoDatabase) {

  /**
   * Gets the name of the database.
   *
   * @return the database name
   */
  lazy val name: String = wrapped.getName

  /**
   * Get the codec registry for the MongoDatabase.
   *
   * @return the { @link org.bson.cn.playscala.mongo.json.codecs.configuration.CodecRegistry}
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
   * Create a new MongoDatabase instance with a different codec registry.
   *
   * @param codecRegistry the new { @link org.bson.cn.playscala.mongo.json.codecs.configuration.CodecRegistry} for the collection
   * @return a new MongoDatabase instance with the different codec registry
   */
  def withCodecRegistry(codecRegistry: CodecRegistry): MongoDatabase =
    MongoDatabase(wrapped.withCodecRegistry(codecRegistry))

  /**
   * Create a new MongoDatabase instance with a different read preference.
   *
   * @param readPreference the new { @link com.mongodb.ReadPreference} for the collection
   * @return a new MongoDatabase instance with the different readPreference
   */
  def withReadPreference(readPreference: ReadPreference): MongoDatabase =
    MongoDatabase(wrapped.withReadPreference(readPreference))

  /**
   * Create a new MongoDatabase instance with a different write concern.
   *
   * @param writeConcern the new { @link com.mongodb.WriteConcern} for the collection
   * @return a new MongoDatabase instance with the different writeConcern
   */
  def withWriteConcern(writeConcern: WriteConcern): MongoDatabase =
    MongoDatabase(wrapped.withWriteConcern(writeConcern))

  /**
   * Create a new MongoDatabase instance with a different read concern.
   *
   * @param readConcern the new [[ReadConcern]] for the collection
   * @return a new MongoDatabase instance with the different ReadConcern
   * @since 1.1
   */
  def withReadConcern(readConcern: ReadConcern): MongoDatabase =
    MongoDatabase(wrapped.withReadConcern(readConcern))

  /**
   * Gets a collection, with a specific default document class.
   *
   * @param collectionName the name of the collection to return
   * @tparam TDocument       the type of the class to use.
   * @return the collection
   */
  def getCollection[TDocument](collectionName: String)(implicit e: TDocument DefaultsTo JsObject, ct: ClassTag[TDocument]): MongoCollection[TDocument] =
    MongoCollection(wrapped.getCollection(collectionName, ct))

  /**
   * Executes command in the context of the current database using the primary server.
   *
   * @param command  the command to be run
   * @tparam TResult the type of the class to use.
   * @return a Observable containing the command result
   */
  def runCommand[TResult](command: Bson)(implicit e: TResult DefaultsTo JsObject, ct: ClassTag[TResult]): Future[TResult] =
    toFuture(wrapped.runCommand[TResult](command, ct, _: SingleResultCallback[TResult]))

  /**
   * Executes command in the context of the current database.
   *
   * @param command        the command to be run
   * @param readPreference the [[ReadPreference]] to be used when executing the command
   * @tparam TResult       the type of the class to use.
   * @return a Observable containing the command result
   */
  def runCommand[TResult](command: Bson, readPreference: ReadPreference)(implicit e: TResult DefaultsTo JsObject, ct: ClassTag[TResult]): Future[TResult] =
    toFuture(wrapped.runCommand(command, readPreference, ct, _: SingleResultCallback[TResult]))

  /**
   * Executes command in the context of the current database using the primary server.
   *
   * @param clientSession the client session with which to associate this operation
   * @param command  the command to be run
   * @tparam TResult the type of the class to use.
   * @return a Observable containing the command result
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def runCommand[TResult](clientSession: ClientSession, command: Bson)(implicit e: TResult DefaultsTo JsObject, ct: ClassTag[TResult]): Future[TResult] =
    toFuture(wrapped.runCommand[TResult](clientSession, command, ct, _: SingleResultCallback[TResult]))

  /**
   * Executes command in the context of the current database.
   *
   * @param command        the command to be run
   * @param readPreference the [[ReadPreference]] to be used when executing the command
   * @tparam TResult       the type of the class to use.
   * @return a Observable containing the command result
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def runCommand[TResult](clientSession: ClientSession, command: Bson, readPreference: ReadPreference)(implicit e: TResult DefaultsTo JsObject, ct: ClassTag[TResult]): Future[TResult] =
    toFuture(wrapped.runCommand(clientSession, command, readPreference, ct, _: SingleResultCallback[TResult]))

  /**
   * Drops this database.
   *
   * [[http://docs.mongodb.org/manual/reference/commands/dropDatabase/#dbcmd.dropDatabase Drop database]]
   * @return a Observable identifying when the database has been dropped
   */
  def drop(): Future[Void] = toFuture(wrapped.drop(_: SingleResultCallback[Void]))

  /**
   * Drops this database.
   *
   * [[http://docs.mongodb.org/manual/reference/commands/dropDatabase/#dbcmd.dropDatabase Drop database]]
   * @param clientSession the client session with which to associate this operation
   * @return a Observable identifying when the database has been dropped
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def drop(clientSession: ClientSession): Future[Void] =
    toFuture(wrapped.drop(clientSession, _: SingleResultCallback[Void]))

  /**
   * Gets the names of all the collections in this database.
   *
   * @return a Observable with all the names of all the collections in this database
   */
  def listCollectionNames(): Future[List[String]] = toFuture(wrapped.listCollectionNames())

  /**
   * Finds all the collections in this database.
   *
   * [[http://docs.mongodb.org/manual/reference/command/listCollections listCollections]]
   * @tparam TResult the target document type of the iterable.
   * @return the fluent list collections interface
   */
  def listCollections[TResult]()(implicit e: TResult DefaultsTo JsObject, ct: ClassTag[TResult]): Future[List[TResult]] =
    toFuture(wrapped.listCollections(ct))

  /**
   * Gets the names of all the collections in this database.
   *
   * @param clientSession the client session with which to associate this operation
   * @return a Observable with all the names of all the collections in this database
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def listCollectionNames(clientSession: ClientSession): Future[List[String]] = toFuture(wrapped.listCollectionNames(clientSession))

  /**
   * Finds all the collections in this database.
   *
   * [[http://docs.mongodb.org/manual/reference/command/listCollections listCollections]]
   * @param clientSession the client session with which to associate this operation
   * @tparam TResult the target document type of the iterable.
   * @return the fluent list collections interface
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def listCollections[TResult](clientSession: ClientSession)(implicit e: TResult DefaultsTo JsObject, ct: ClassTag[TResult]): Future[List[TResult]] =
    toFuture(wrapped.listCollections(clientSession, ct))

  /**
   * Create a new collection with the given name.
   *
   * [[http://docs.mongodb.org/manual/reference/commands/create Create Command]]
   * @param collectionName the name for the new collection to create
   * @return a Observable identifying when the collection has been created
   */
  def createCollection(collectionName: String): Future[Void] =
    toFuture(wrapped.createCollection(collectionName, _: SingleResultCallback[Void]))

  /**
   * Create a new collection with the selected options
   *
   * [[http://docs.mongodb.org/manual/reference/commands/create Create Command]]
   * @param collectionName the name for the new collection to create
   * @param options        various options for creating the collection
   * @return a Observable identifying when the collection has been created
   */
  def createCollection(collectionName: String, options: CreateCollectionOptions): Future[Void] =
    toFuture(wrapped.createCollection(collectionName, options, _: SingleResultCallback[Void]))

  /**
   * Create a new collection with the given name.
   *
   * [[http://docs.mongodb.org/manual/reference/commands/create Create Command]]
   * @param clientSession the client session with which to associate this operation
   * @param collectionName the name for the new collection to create
   * @return a Observable identifying when the collection has been created
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def createCollection(clientSession: ClientSession, collectionName: String): Future[Void] =
    toFuture(wrapped.createCollection(clientSession, collectionName, _: SingleResultCallback[Void]))

  /**
   * Create a new collection with the selected options
   *
   * [[http://docs.mongodb.org/manual/reference/commands/create Create Command]]
   * @param clientSession the client session with which to associate this operation
   * @param collectionName the name for the new collection to create
   * @param options        various options for creating the collection
   * @return a Observable identifying when the collection has been created
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def createCollection(clientSession: ClientSession, collectionName: String, options: CreateCollectionOptions): Future[Void] =
    toFuture(wrapped.createCollection(clientSession, collectionName, options, _: SingleResultCallback[Void]))

  /**
   * Creates a view with the given name, backing collection/view name, and aggregation pipeline that defines the view.
   *
   * [[http://docs.mongodb.org/manual/reference/commands/create Create Command]]
   * @param viewName the name of the view to create
   * @param viewOn   the backing collection/view for the view
   * @param pipeline the pipeline that defines the view
   * @since 1.2
   * @note Requires MongoDB 3.4 or greater
   */
  def createView(viewName: String, viewOn: String, pipeline: Seq[Bson]): Future[Void] =
    toFuture(wrapped.createView(viewName, viewOn, pipeline.asJava, _: SingleResultCallback[Void]))

  /**
   * Creates a view with the given name, backing collection/view name, aggregation pipeline, and options that defines the view.
   *
   * [[http://docs.mongodb.org/manual/reference/commands/create Create Command]]
   * @param viewName          the name of the view to create
   * @param viewOn            the backing collection/view for the view
   * @param pipeline          the pipeline that defines the view
   * @param createViewOptions various options for creating the view
   * @since 1.2
   * @note Requires MongoDB 3.4 or greater
   */
  def createView(viewName: String, viewOn: String, pipeline: Seq[Bson], createViewOptions: CreateViewOptions): Future[Void] =
    toFuture(wrapped.createView(viewName, viewOn, pipeline.asJava, createViewOptions, _: SingleResultCallback[Void]))

  /**
   * Creates a view with the given name, backing collection/view name, and aggregation pipeline that defines the view.
   *
   * [[http://docs.mongodb.org/manual/reference/commands/create Create Command]]
   * @param clientSession the client session with which to associate this operation
   * @param viewName the name of the view to create
   * @param viewOn   the backing collection/view for the view
   * @param pipeline the pipeline that defines the view
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def createView(clientSession: ClientSession, viewName: String, viewOn: String, pipeline: Seq[Bson]): Future[Void] =
    toFuture(wrapped.createView(clientSession, viewName, viewOn, pipeline.asJava, _: SingleResultCallback[Void]))

  /**
   * Creates a view with the given name, backing collection/view name, aggregation pipeline, and options that defines the view.
   *
   * [[http://docs.mongodb.org/manual/reference/commands/create Create Command]]
   * @param clientSession the client session with which to associate this operation
   * @param viewName          the name of the view to create
   * @param viewOn            the backing collection/view for the view
   * @param pipeline          the pipeline that defines the view
   * @param createViewOptions various options for creating the view
   * @since 2.2
   * @note Requires MongoDB 3.6 or greater
   */
  def createView(clientSession: ClientSession, viewName: String, viewOn: String, pipeline: Seq[Bson],
                 createViewOptions: CreateViewOptions): Future[Void] =
    toFuture(wrapped.createView(clientSession, viewName, viewOn, pipeline.asJava, createViewOptions, _: SingleResultCallback[Void]))
}
