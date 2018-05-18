package cn.playscala.mongo

import javax.inject.{Inject, Provider}
import play.api.{Environment, Mode}
import play.api.inject.ApplicationLifecycle
import scala.concurrent.Future

class MongoProvider(config: MongoConfig) extends Provider[Mongo] {
  @Inject private var applicationLifecycle: ApplicationLifecycle = _
  @Inject private var env: Environment = _

  lazy val get: Mongo = {

    if (env.mode == Mode.Dev) {
      Mongo.isDevMode = true
    }

    val default = Mongo(config)
    applicationLifecycle.addStopHook(() => Future.successful(default.close()))
    default
  }
}