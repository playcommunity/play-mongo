package cn.playscala.mongo.gridfs

import java.util.Date
import com.mongodb.client.gridfs.model.{GridFSFile => JGridFSFile}
import org.bson.{BsonObjectId, BsonString, BsonValue, Document}
import play.api.libs.json.{JsObject, Json}

case class GridFSFile(wrapped: JGridFSFile, gridFSBucket: GridFSBucket) {
  /**
    * The {@link BsonValue} id for this file.
    *
    * @return the id for this file
    */
  def getId: String = wrapped.getId match {
    case s: BsonString => s.getValue
    case o: BsonObjectId => o.getValue.toHexString
  }

  /**
    * The filename
    *
    * @return the filename
    */
  def getFilename: String = wrapped.getFilename

  /**
    * The length, in bytes of this file
    *
    * @return the length, in bytes of this file
    */
  def getLength: Long = wrapped.getLength

  /**
    * The size, in bytes, of each data chunk of this file
    *
    * @return the size, in bytes, of each data chunk of this file
    */
  def getChunkSize: Int = wrapped.getChunkSize

  /**
    * The date and time this file was added to GridFS
    *
    * @return the date and time this file was added to GridFS
    */
  def getUploadDate: Date = wrapped.getUploadDate

  /**
    * The hash of the contents of the stored file
    *
    * @return the hash of the contents of the stored file
    */
  def getMD5: String = wrapped.getMD5

  /**
    * Any additional metadata stored along with the file
    *
    * @return the metadata document or null
    */
  def getMetadata: JsObject = Json.parse(wrapped.getMetadata.toJson()).as[JsObject]

  /**
    * The content type of the file
    *
    * @return the content type of the file
    */
  def getContentType: String = {
    val metadata = wrapped.getMetadata
    if (metadata != null && metadata.containsKey("contentType")) {
      metadata.getString("contentType")
    } else {
      ""
    }
  }

  def stream: GridFSDownloadStream = gridFSBucket.openDownloadStream(getId)

}
