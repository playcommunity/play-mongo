package cn.playscala.mongo.gridfs

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.gridfs.{GridFSDownloadStream => JGridFSDownloadStream}
import cn.playscala.mongo.internal.AsyncResultHelper._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Logger

import scala.util.control.NonFatal

class GridFSDownloadStream(wrapped: JGridFSDownloadStream) {
  private val isReadable = new AtomicBoolean(true)
  private val DEFAULT_BATCH_SIZE = 261120

  def toIterator: Iterator[Future[ByteBuffer]] = {
    wrapped.batchSize(DEFAULT_BATCH_SIZE)

    new Iterator[Future[ByteBuffer]]() {
      override def hasNext: Boolean = isReadable.get()
      override def next(): Future[ByteBuffer] = {
        val buffer = ByteBuffer.allocate(DEFAULT_BATCH_SIZE)
        toFuture(wrapped.read(buffer, _: SingleResultCallback[Integer])).map{ read =>
          if (read == -1) {
            isReadable.set(false)
            wrapped.close((_, _) => {})
            ByteBuffer.allocate(0)
          } else {
            buffer.flip()
            buffer
          }
        }
      }
    }
  }

  def toSource: Source[ByteString, _] = {
    Source
      .fromIterator(() => toIterator)
      //.mapAsync(1)(identity)
      .mapAsync(1)(f => f.recover{
        case NonFatal(t) =>
          Logger.error(s"Read GridFSDownloadStream Error: ${t.getMessage}", t)
          ByteBuffer.allocate(0)
      })
      .filter(_.hasRemaining)
      .map(ByteString.apply)
  }
}
