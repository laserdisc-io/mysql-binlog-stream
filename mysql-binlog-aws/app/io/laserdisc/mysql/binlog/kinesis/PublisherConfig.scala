package io.laserdisc.mysql.binlog.kinesis

import cats.effect.{ Async, Sync }
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.aws.internal.{ KinesisProducerClient, KinesisProducerClientImpl }
import io.laserdisc.mysql.binlog.config.BinLogConfig
import org.typelevel.log4cats.Logger
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

import scala.concurrent.duration.FiniteDuration

case class PublisherConfig[F[_]: Async](
  binlogConfig: BinLogConfig,
  checkpointAppName: String,
  checkpointEvery: FiniteDuration,
  checkpointTableName: String,
  checkpointTableRegion: Region,
  kinesisOutputStream: String,
  kinesisRegion: Region
) {

  def createKinesisProducer(implicit logger: Logger[F]): F[KinesisProducerClient[F]] =
    for {
      kpc <- Async[F].delay(new KinesisProducerClientImpl[F] {
               override val region: Option[String] = Some(kinesisRegion.id)
             })
      _ <- logger.info(s"KINESIS-PRODUCER-INIT region:${kinesisRegion.id}")
    } yield kpc

  def createDynamoDBClient: F[DynamoDbAsyncClient] =
    Sync[F].delay(DynamoDbAsyncClient.builder().region(checkpointTableRegion).build())

  // TODO:
//  override def toString: String =
//    s"""AppConfig(env:$env, appName:$appName, kinesisStream:$kinesisStreamName checkpointEvery:$checkpointEvery ddbOffsetTable=$ddbOffsetTable, $binlogConfig)"""
}
