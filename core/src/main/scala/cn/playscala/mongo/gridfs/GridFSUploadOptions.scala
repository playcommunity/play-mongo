package cn.playscala.mongo.gridfs

import org.bson.Document
import play.api.libs.json.JsObject

object GridFSUploadOptions {
  def apply(): GridFSUploadOptions = new GridFSUploadOptions

  def apply(chunkSizeBytes: Int): GridFSUploadOptions =
    new GridFSUploadOptions().chunkSizeBytes(chunkSizeBytes)

  def apply(metadata: JsObject): GridFSUploadOptions =
    new GridFSUploadOptions().metadata(Document.parse(metadata.toString()))

  def apply(chunkSizeBytes: Int, metadata: JsObject): GridFSUploadOptions =
    new GridFSUploadOptions().chunkSizeBytes(chunkSizeBytes).metadata(Document.parse(metadata.toString()))
}
