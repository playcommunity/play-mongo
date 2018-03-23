package cn.playscala.mongo.internal

import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.MongoIterable

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Future, Promise}

object AsyncResultHelper {

  def toFuture[TResult](block: SingleResultCallback[TResult] => Unit): Future[TResult] = {
    val promise = Promise[TResult]()
    block { (document: TResult, t: Throwable) =>
      if (t != null) {
        promise.failure(t)
      } else {
        promise.success(document)
      }
    }
    promise.future
  }

  def toFuture[TResult](iterable: MongoIterable[TResult]): Future[List[TResult]] = {
    val promise = Promise[List[TResult]]()
    iterable.into(ListBuffer[TResult]().asJava, (result: java.util.List[TResult], t: Throwable) => {
      if (t != null) {
        promise.failure(t)
      } else {
        promise.success(result.asScala.toList)
      }
    })
    promise.future
  }

}
