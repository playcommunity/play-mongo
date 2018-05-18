package cn.playscala.mongo.internal

import java.nio.ByteBuffer
import org.bson.{BsonBinaryReader, BsonBinaryWriter, BsonDocument, ByteBufNIO}
import org.bson.codecs.{BsonDocumentCodec, Codec, DecoderContext, EncoderContext}
import org.bson.io.{BasicOutputBuffer, ByteBufferBsonInput}

object CodecHelper {
  val bsonDocumentCodec = new BsonDocumentCodec()

  def decodeBsonDocument[C](bsonDocument: BsonDocument, codec: Codec[C]): C = {
    val buffer = new BasicOutputBuffer
    val writer = new BsonBinaryWriter(buffer)
    bsonDocumentCodec.encode(writer, bsonDocument, EncoderContext.builder.build)

    val reader = new BsonBinaryReader(new ByteBufferBsonInput(new ByteBufNIO(ByteBuffer.wrap(buffer.toByteArray))))
    codec.decode(reader, DecoderContext.builder.build)
  }

}
