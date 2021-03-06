package io.kafkamate
package kafka

import zio._
import zio.blocking._
import zio.kafka.serde._
import zio.kafka.producer._
import zio.macros.accessible

import config._, ClustersConfig._

@accessible object KafkaProducer {
  type KafkaProducer = Has[Service]

  trait Service {
    def produce(topic: String, key: String, value: String)(clusterId: String): RIO[Blocking, Unit]
  }

  lazy val liveLayer: URLayer[ClustersConfigService, KafkaProducer] =
    ZLayer.fromService { clusterConfigService =>
      new Service {
        lazy val serdeLayer: ULayer[Has[Serializer[Any, String]]] =
          ZLayer.succeed(Serde.string)

        def settingsLayer(clusterId: String): TaskLayer[Has[ProducerSettings]] =
          clusterConfigService
            .getCluster(clusterId)
            .map(c => ProducerSettings(c.kafkaHosts))
            .toLayer

        def producerLayer(clusterId: String): RLayer[Blocking, Producer[Any, String, String]] =
          Blocking.any ++ serdeLayer ++ settingsLayer(clusterId) >>> Producer.live[Any, String, String]

        def produce(topic: String, key: String, value: String)(clusterId: String): RIO[Blocking, Unit] =
          Producer
            .produce[Any, String, String](topic, key, value)
            .unit
            .provideSomeLayer[Blocking](producerLayer(clusterId))
      }
    }

}
