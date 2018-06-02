package cn.playscala.mongo.gridfs

import java.util.concurrent.TimeUnit
import com.mongodb.async.client.gridfs.GridFSFindIterable
import scala.concurrent.duration.Duration
import cn.playscala.mongo._
import cn.playscala.mongo.internal.AsyncResultHelper.{toFuture, toOptionFuture}
import com.mongodb.async.SingleResultCallback
import play.api.libs.json.{JsBoolean, JsObject}
import scala.concurrent.Future
import com.mongodb.client.gridfs.model.{GridFSFile => JGridFSFile}
import scala.concurrent.ExecutionContext.Implicits.global

case class GridFSFindBuilder(private val wrapped: GridFSFindIterable, private val gridFSBucket: GridFSBucket) {

  /**
    * Helper to return a Future limited to just the first result the query.
    *
    * **Note:** Sets limit in the background so only returns 1.
    *
    * @return a future which will return the first item
    */
  def first: Future[Option[GridFSFile]] = toOptionFuture(wrapped.first(_: SingleResultCallback[JGridFSFile])).map(_.map(f => GridFSFile(f, gridFSBucket)))

  /**
    * Execute the query and return the query results.
    * @return A future of result list.
    */
  def list(): Future[List[GridFSFile]] = toFuture(wrapped).map(_.map(f => GridFSFile(f, gridFSBucket)))

  /**
    * Sets the query filter to apply to the query.
    *
    * Below is an example of filtering against the filename and some nested metadata that can also be stored along with the file data:
    *
    * {{{
    * Filters.and(Filters.eq("filename", "mongodb.png"), Filters.eq("metadata.contentType", "image/png"));
    * }}}
    *
    * @param filter the filter, which may be null.
    * @return this
    * @see [[http://docs.mongodb.org/manual/reference/method/db.collection.find/ Filter]]
    */
  def filter(filter: JsObject): GridFSFindBuilder = {
    wrapped.filter(filter)
    this
  }

  /**
    * Sets the limit to apply.
    *
    * @param limit the limit, which may be null
    * @return this
    * @see [[http://docs.mongodb.org/manual/reference/method/cursor.limit/#cursor.limit Limit]]
    */
  def limit(limit: Int): GridFSFindBuilder = {
    wrapped.limit(limit)
    this
  }

  /**
    * Sets the number of documents to skip.
    *
    * @param skip the number of documents to skip
    * @return this
    * @see [[http://docs.mongodb.org/manual/reference/method/cursor.skip/#cursor.skip Skip]]
    */
  def skip(skip: Int): GridFSFindBuilder = {
    wrapped.skip(skip)
    this
  }

  /**
    * Sets the sort criteria to apply to the query.
    *
    * @param sort the sort criteria, which may be null.
    * @return this
    * @see [[http://docs.mongodb.org/manual/reference/method/cursor.sort/ Sort]]
    */
  def sort(sort: JsObject): GridFSFindBuilder = {
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
  def noCursorTimeout(noCursorTimeout: Boolean): GridFSFindBuilder = {
    wrapped.noCursorTimeout(noCursorTimeout)
    this
  }

  /**
    * Sets the maximum execution time on the server for this operation.
    *
    * @see [[http://docs.mongodb.org/manual/reference/operator/meta/maxTimeMS/ Max Time]]
    * @param duration the duration
    * @return this
    */
  def maxTime(duration: Duration): GridFSFindBuilder = {
    wrapped.maxTime(duration.toMillis, TimeUnit.MILLISECONDS)
    this
  }

  /**
    * Sets the number of documents to return per batch.
    *
    * @param batchSize the batch size
    * @return this
    * @see [[http://docs.mongodb.org/manual/reference/method/cursor.batchSize/#cursor.batchSize Batch Size]]
    */
  def batchSize(batchSize: Int): GridFSFindBuilder = {
    wrapped.batchSize(batchSize)
    this
  }
}
