package cn.playscala.mongo

import play.api.inject.Module
import play.api.{Configuration, Environment}


class MongoModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    val configSeq = MongoConfig.parse(configuration)
    configSeq.map{ config =>
      val provider = new MongoProvider(config)
      if (config.name == "default") {
        bind[Mongo].to(provider)
      } else {
        bind[Mongo].qualifiedWith(config.name).to(provider)
      }
    }
  }

}
