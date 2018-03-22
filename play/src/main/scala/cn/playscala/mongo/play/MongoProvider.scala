package cn.playscala.mongo.play

import scala.concurrent.Future
import javax.inject.{Inject, Provider}
import play.api.inject.ApplicationLifecycle

class MongoProvider(config: MongoConfig) extends Provider[Mongo] {
  @Inject private var applicationLifecycle: ApplicationLifecycle = _

  lazy val get: Mongo = {
    val default = new DefaultMongo(config)
    applicationLifecycle.addStopHook(() => Future.successful(default.close()))
    default
  }
}