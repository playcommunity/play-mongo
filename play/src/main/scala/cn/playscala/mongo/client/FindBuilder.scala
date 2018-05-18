package cn.playscala.mongo.client

import java.util.concurrent.TimeUnit

import cn.playscala.mongo.MongoCollection
import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.FindIterable
import play.api.libs.json.JsObject
import cn.playscala.mongo.codecs.Implicits.toBsonDocument

import scala.concurrent.Future
import cn.playscala.mongo.internal.AsyncResultHelper._
import com.mongodb.CursorType

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.collection.mutable
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
  *
  * @param wrapped
  * @param collection
  * @tparam TResult the target result type.
  */
case class FindBuilder[TResult:ClassTag:TypeTag](private val wrapped: FindIterable[TResult], collection: MongoCollection[_])(implicit ct: ClassTag[TResult], tt: TypeTag[TResult]) {
  val appliedOperations = mutable.Map.empty[String, Any]

  def fetch[R](field: String)(implicit ct: ClassTag[R], tt: TypeTag[R]): AggregateBuilder[TResult, R] = {
    AggregateBuilder[TResult, R](collection, field)
  }

  /**
    * Execute the query and return the query results.
    * @return A future of result list.
    */
  def list(): Future[List[TResult]] = toFuture(wrapped)

  /**
    * Helper to return a Future limited to just the first result the query.
    *
    * **Note:** Sets limit in the background so only returns 1.
    *
    * @return a future which will return the first item
    */
  def first(): Future[Option[TResult]] = toOptionFuture(wrapped.first(_: SingleResultCallback[TResult]))

  /**
    * Sets the query filter to apply to the query.
    *
    * [[http://docs.mongodb.org/manual/reference/method/db.collection.find/ Filter]]
    * @param filter the filter, which may be null.
    * @return this
    */
  def filter(filter: JsObject): FindBuilder[TResult] = {
    appliedOperations("filter") = filter
    wrapped.filter(filter)
    this
  }

  /**
    * Sets the limit to apply.
    *
    * [[http://docs.mongodb.org/manual/reference/method/cursor.limit/#cursor.limit Limit]]
    * @param limit the limit, which may be null
    * @return this
    */
  def limit(limit: Int): FindBuilder[TResult] = {
    appliedOperations("limit") = limit
    wrapped.limit(limit)
    this
  }

  /**
    * Sets the number of documents to skip.
    *
    * [[http://docs.mongodb.org/manual/reference/method/cursor.skip/#cursor.skip Skip]]
    * @param skip the number of documents to skip
    * @return this
    */
  def skip(skip: Int): FindBuilder[TResult] = {
    appliedOperations("skip") = skip
    wrapped.skip(skip)
    this
  }

  /**
    * Sets the maximum execution time on the server for this operation.
    *
    * [[http://docs.mongodb.org/manual/reference/operator/meta/maxTimeMS/ Max Time]]
    * @param duration the duration
    * @return this
    */
  def maxTime(duration: Duration): FindBuilder[TResult] = {
    appliedOperations("maxTime") = duration
    wrapped.maxTime(duration.toMillis, TimeUnit.MILLISECONDS)
    this
  }

  /**
    * The maximum amount of time for the server to wait on new documents to satisfy a tailable cursor
    * query. This only applies to a TAILABLE_AWAIT cursor. When the cursor is not a TAILABLE_AWAIT cursor,
    * this option is ignored.
    *
    * On servers &gt;= 3.2, this option will be specified on the getMore command as "maxTimeMS". The default
    * is no value: no "maxTimeMS" is sent to the server with the getMore command.
    *
    * On servers &lt; 3.2, this option is ignored, and indicates that the driver should respect the server's default value
    *
    * A zero value will be ignored.
    *
    * [[http://docs.mongodb.org/manual/reference/operator/meta/maxTimeMS/ Max Time]]
    * @param duration the duration
    * @return the maximum await execution time in the given time unit
    * @since 1.1
    */
  def maxAwaitTime(duration: Duration): FindBuilder[TResult] = {
    appliedOperations("maxAwaitTime") = duration
    wrapped.maxAwaitTime(duration.toMillis, TimeUnit.MILLISECONDS)
    this
  }

  /**
    * Sets the query modifiers to apply to this operation.
    *
    * [[http://docs.mongodb.org/manual/reference/operator/query-modifier/ Query Modifiers]]
    *
    * @param modifiers the query modifiers to apply, which may be null.
    * @return this
    * @deprecated use the individual setters instead
    */
  @Deprecated
  def modifiers(modifiers: JsObject): FindBuilder[TResult] = {
    wrapped.modifiers(modifiers)
    this
  }

  /**
    * Sets a document describing the fields to return for all matching documents.
    *
    * [[http://docs.mongodb.org/manual/reference/method/db.collection.find/ Projection]]
    * @param projection the project document, which may be null.
    * @return this
    */
  def projection(projection: JsObject): FindBuilder[TResult] = {
    appliedOperations("projection") = projection
    wrapped.projection(projection)
    this
  }

  /**
    * Sets the sort criteria to apply to the query.
    *
    * [[http://docs.mongodb.org/manual/reference/method/cursor.sort/ Sort]]
    * @param sort the sort criteria, which may be null.
    * @return this
    */
  def sort(sort: JsObject): FindBuilder[TResult] = {
    appliedOperations("sort") = sort
    wrapped.sort(sort)
    this
  }

  /**
    * The server normally times out idle cursors after an inactivity period (10 minutes)
    * to prevent excess memory use. Set this option to prevent that.
    *
    * @param noCursorTimeout true if cursor timeout is disabled
    * @return this
    */
  def noCursorTimeout(noCursorTimeout: Boolean): FindBuilder[TResult] = {
    wrapped.noCursorTimeout(noCursorTimeout)
    this
  }

  /**
    * Users should not set this under normal circumstances.
    *
    * @param oplogReplay if oplog replay is enabled
    * @return this
    */
  def oplogReplay(oplogReplay: Boolean): FindBuilder[TResult] = {
    wrapped.oplogReplay(oplogReplay)
    this
  }

  /**
    * Get partial results from a sharded cluster if one or more shards are unreachable (instead of throwing an error).
    *
    * @param partial if partial results for sharded clusters is enabled
    * @return this
    */
  def partial(partial: Boolean): FindBuilder[TResult] = {
    wrapped.partial(partial)
    this
  }

  /**
    * Sets the cursor type.
    *
    * @param cursorType the cursor type
    * @return this
    */
  def cursorType(cursorType: CursorType): FindBuilder[TResult] = {
    wrapped.cursorType(cursorType)
    this
  }

  /**
    * Sets the collation options
    *
    * @param collation the collation options to use
    * @return this
    * @since 1.2
    * @note A null value represents the server default.
    * @note Requires MongoDB 3.4 or greater
    */
  def collation(collation: Collation): FindBuilder[TResult] = {
    wrapped.collation(collation)
    this
  }

  /**
    * Sets the comment to the query. A null value means no comment is set.
    *
    * @param comment the comment
    * @return this
    * @since 2.2
    */
  def comment(comment: String): FindBuilder[TResult] = {
    wrapped.comment(comment)
    this
  }

  /**
    * Sets the hint for which index to use. A null value means no hint is set.
    *
    * @param hint the hint
    * @return this
    * @since 2.2
    */
  def hint(hint: JsObject): FindBuilder[TResult] = {
    wrapped.hint(hint)
    this
  }

  /**
    * Sets the exclusive upper bound for a specific index. A null value means no max is set.
    *
    * @param max the max
    * @return this
    * @since 2.2
    */
  def max(max: JsObject): FindBuilder[TResult] = {
    appliedOperations("max") = max
    wrapped.max(max)
    this
  }

  /**
    * Sets the minimum inclusive lower bound for a specific index. A null value means no max is set.
    *
    * @param min the min
    * @return this
    * @since 2.2
    */
  def min(min: JsObject): FindBuilder[TResult] = {
    appliedOperations("min") = min
    wrapped.min(min)
    this
  }

  /**
    * Sets the maximum number of documents or index keys to scan when executing the query.
    *
    * A zero value or less will be ignored, and indicates that the driver should respect the server's default value.
    *
    * @param maxScan the maxScan
    * @return this
    * @since 2.2
    */
  def maxScan(maxScan: Long): FindBuilder[TResult] = {
    appliedOperations("maxScan") = maxScan
    wrapped.maxScan(maxScan)
    this
  }

  /**
    * Sets the returnKey. If true the find operation will return only the index keys in the resulting documents.
    *
    * @param returnKey the returnKey
    * @return this
    * @since 2.2
    */
  def returnKey(returnKey: Boolean): FindBuilder[TResult] = {
    wrapped.returnKey(returnKey)
    this
  }

  /**
    * Sets the showRecordId. Set to true to add a field `\$recordId` to the returned documents.
    *
    * @param showRecordId the showRecordId
    * @return this
    * @since 2.2
    */
  def showRecordId(showRecordId: Boolean): FindBuilder[TResult] = {
    wrapped.showRecordId(showRecordId)
    this
  }

  /**
    * Sets the snapshot.
    *
    * If true it prevents the cursor from returning a document more than once because of an intervening write operation.
    *
    * @param snapshot the snapshot
    * @return this
    * @since 2.2
    */
  def snapshot(snapshot: Boolean): FindBuilder[TResult] = {
    wrapped.snapshot(snapshot)
    this
  }
}
