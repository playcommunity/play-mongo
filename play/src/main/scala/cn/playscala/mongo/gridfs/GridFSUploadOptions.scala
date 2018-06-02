package cn.playscala.mongo.gridfs

import org.bson.Document
import play.api.libs.json.{JsObject, Json}
import com.mongodb.client.gridfs.model.{GridFSUploadOptions => JGridFSUploadOptions}

object GridFSUploadOptions {
  val DEFAULT_CHUNK_SIZE = 261120

  def apply(): GridFSUploadOptions = new GridFSUploadOptions

  def apply(chunkSize: Int): GridFSUploadOptions =
    new GridFSUploadOptions(chunkSize)

  def apply(metadata: JsObject): GridFSUploadOptions =
    new GridFSUploadOptions(metadata)

  def apply(chunkSize: Int, metadata: JsObject): GridFSUploadOptions =
    new GridFSUploadOptions(metadata, chunkSize)
}

case class GridFSUploadOptions() {
  import GridFSUploadOptions._

  val wrapped = new JGridFSUploadOptions
  wrapped.chunkSizeBytes(DEFAULT_CHUNK_SIZE)
  wrapped.metadata(new Document())

  def this(metadata: JsObject) = {
    this()
    wrapped.metadata(Document.parse(Json.stringify(metadata)))
  }

  def this(chunkSize: Int) = {
    this()
    wrapped.chunkSizeBytes(chunkSize)
  }

  def this(metadata: JsObject, chunkSize: Int) = {
    this(metadata)
    wrapped.chunkSizeBytes(chunkSize)
  }
}