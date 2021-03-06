package io.kafkamate
package config

import zio._
import zio.system
import system.System

object ConfigPathService {

  lazy val EnvKey   = "KAFKAMATE_ENV"
  lazy val FileName = "kafkamate.json"

  case class ConfigPath(path: os.Path)

  type HasConfigPath = Has[ConfigPath]

  lazy val liveLayer: URLayer[System, HasConfigPath] =
    system
      .env(EnvKey)
      .map {
        case Some("prod") => ConfigPath(os.root / FileName)
        case _            => ConfigPath(os.pwd / FileName)
      }
      .toLayer
      .orDie

}
