package cn.playscala.mongo.gridfs

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.gridfs.{GridFSDownloadStream => JGridFSDownloadStream}
import com.mongodb.client.gridfs.model.GridFSFile
import cn.playscala.mongo.internal.AsyncResultHelper._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.mongodb.client.gridfs.model.{GridFSFile => JGridFSFile}

class GridFSDownloadStream(wrapped: JGridFSDownloadStream) {
  private val isReadable = new AtomicBoolean(true)
  private val DEFAULT_BATCH_SIZE = 255 * 1024

  def getGridFSFile: Future[GridFSFile] = {
    toFuture(wrapped.getGridFSFile(_: SingleResultCallback[JGridFSFile])).map(f => new GridFSFile(f))
  }

  def toIterator: Iterator[Future[ByteBuffer]] = {
    new Iterator[Future[ByteBuffer]]() {
      override def hasNext: Boolean = isReadable.get()
      override def next(): Future[ByteBuffer] = {
        val buffer = ByteBuffer.allocate(DEFAULT_BATCH_SIZE)
        toFuture(wrapped.read(buffer, _: SingleResultCallback[Integer])).map{ read =>
          if (read == -1) {
            isReadable.set(false)
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
      .mapAsync(1)(identity)
      .map(ByteString.apply)
  }
}
