import cn.playscala.mongo.Mongo
import cn.playscala.mongo.codecs.macrocodecs.JsonFormat

package object dummy_models {
  @JsonFormat("dummy_models")
  implicit val formats = ???

  Mongo.setModelsPackage("dummy_models")
}
