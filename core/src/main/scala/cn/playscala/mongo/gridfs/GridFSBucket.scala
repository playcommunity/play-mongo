package cn.playscala.mongo.gridfs

import java.io.{File, FileInputStream, InputStream}

import cn.playscala.mongo.MongoDatabase
import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.gridfs.helpers.AsyncStreamHelper.toAsyncInputStream
import com.mongodb.async.client.gridfs.{GridFSBuckets, GridFSBucket => JGridFSBucket}
import com.mongodb.session.ClientSession
import org.bson.BsonString
import org.bson.types.ObjectId

import scala.concurrent.Future
import cn.playscala.mongo.internal.AsyncResultHelper._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * A factory for GridFSBucket instances.
  *
  * @since 1.2
  */
object GridFSBucket {

  /**
    * Create a new GridFS bucket with the default `'fs'` bucket name
    *
    * @param database the database instance to use with GridFS
    * @return the GridFSBucket
    */
  def apply(database: MongoDatabase): GridFSBucket = new GridFSBucket(GridFSBuckets.create(database.wrapped))

  /**
    * Create a new GridFS bucket with a custom bucket name
    *
    * @param database   the database instance to use with GridFS
    * @param bucketName the custom bucket name to use
    * @return the GridFSBucket
    */
  def apply(database: MongoDatabase, bucketName: String): GridFSBucket = new GridFSBucket(GridFSBuckets.create(database.wrapped, bucketName))
}

class GridFSBucket(val wrapped: JGridFSBucket) {

  /**
    * Upload a file to GridFS
    *
    * @param clientSession   the client session with which to associate this operation
    * @param fileId   the custom id of the file
    * @param fileName   the custom name of the file
    * @param file   the file to upload
    * @param options  the GridFSUploadOptions
    * @return the file_id
    */
  def uploadFromFile(clientSession: Option[ClientSession] = None, fileId: Option[String] = None, fileName: Option[String] = None, file: File, options: Option[GridFSUploadOptions] = None): Future[String] = {
    val file_id = fileId.getOrElse(ObjectId.get().toHexString)
    val file_name = fileName.getOrElse(file.getName)
    val upload_options = options.getOrElse(GridFSUploadOptions())

    toFuture[Void](clientSession match {
      case Some(cs) =>
        wrapped.uploadFromStream(cs, new BsonString(file_id), file_name, toAsyncInputStream(new FileInputStream(file)), upload_options, _: SingleResultCallback[Void])
      case None =>
        wrapped.uploadFromStream(new BsonString(file_id), file_name, toAsyncInputStream(new FileInputStream(file)), upload_options, _: SingleResultCallback[Void])
    }).map(_ => file_id)
  }

  /**
    * Upload a file to GridFS
    *
    * @param clientSession   the client session with which to associate this operation
    * @param fileId   the custom id of the file
    * @param fileName   the custom name of the file
    * @param inputStream   the input stream to upload
    * @param options  the GridFSUploadOptions
    * @return the file_id
    */
  def uploadFromInputStream(clientSession: Option[ClientSession] = None, fileId: Option[String] = None, fileName: Option[String] = None, inputStream: InputStream, options: Option[GridFSUploadOptions] = None): Future[String] = {
    val file_id = fileId.getOrElse(ObjectId.get().toHexString)
    val file_name = fileName.getOrElse("")
    val upload_options = options.getOrElse(new GridFSUploadOptions)

    toFuture[Void](clientSession match {
      case Some(cs) =>
        wrapped.uploadFromStream(cs, new BsonString(file_id), file_name, toAsyncInputStream(inputStream), upload_options, _: SingleResultCallback[Void])
      case None =>
        wrapped.uploadFromStream(new BsonString(file_id), file_name, toAsyncInputStream(inputStream), upload_options, _: SingleResultCallback[Void])
    }).map(_ => file_id)
  }

  /**
    * Opens a GridFSDownloadStreamIterator from which the application can read the contents of the stored file specified by {@code id}.
    *
    * @param id the custom id value of the file, to be put into a stream.
    * @return the stream
    */
  def openDownloadStream(id: String) = new GridFSDownloadStreamIterator(wrapped.openDownloadStream(new BsonString(id)))

}
