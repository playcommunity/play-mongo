package cn.playscala.mongo.gridfs

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.gridfs.GridFSDownloadStream
import com.mongodb.client.gridfs.model.GridFSFile
import cn.playscala.mongo.internal.AsyncResultHelper._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class GridFSDownloadStreamIterator(wrapped: GridFSDownloadStream) extends Iterator[Future[ByteBuffer]] {
  private val isReadable = new AtomicBoolean(true)
  private val DEFAULT_BATCH_SIZE = 255 * 1024

  override def hasNext: Boolean = isReadable.get()

  override def next(): Future[ByteBuffer] = {
    val buffer = ByteBuffer.allocate(DEFAULT_BATCH_SIZE)
    toFuture(wrapped.read(buffer, _: SingleResultCallback[Integer])).map{ read =>
      if (read == -1) {
        isReadable.set(false)
      }
      buffer
    }
  }

  def getGridFSFile: Future[GridFSFile] = {
    toFuture(wrapped.getGridFSFile(_: SingleResultCallback[GridFSFile]))
  }
}
