package cn.playscala.mongo

import javax.inject.{Inject, Provider}

import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

class MongoProvider(config: MongoConfig) extends Provider[Mongo] {
  @Inject private var applicationLifecycle: ApplicationLifecycle = _

  lazy val get: Mongo = {
    val default = new DefaultMongo(config)
    applicationLifecycle.addStopHook(() => Future.successful(default.close()))
    default
  }
}