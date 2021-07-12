package io.laserdisc.mysql.binlog.kinesis
import cats.effect.{ Async, Sync }
import cats.implicits._
import fs2.Pipe
import io.laserdisc.mysql.binlog.checkpoint.BinlogOffset
import org.scanamo.generic.auto._
import org.scanamo.syntax._
import org.scanamo.{ DynamoReadError, ScanamoCats, Table }
import org.typelevel.log4cats.Logger
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

object Checkpointing {

  def fetchOffset[F[_]: Async](
    dynamoDBClient: DynamoDbAsyncClient,
    appConfig: KinesisPublisherConfig
  ): F[Option[BinlogOffset]] = {
    val offsets = Table[BinlogOffset](appConfig.checkpointTableName)
    println(s"LOADING OFFSETS FROM $offsets")
    ScanamoCats[F](dynamoDBClient)
      .exec(offsets.get("appName" === appConfig.checkpointAppName))
      .flatMap {
        case Some(value) =>
          value
            .leftMap(e => new RuntimeException(DynamoReadError.describe(e)))
            .liftTo[F]
            .map(Some(_))
        case None => Sync[F].pure(None)
      }
  }

  def checkpoint[F[_]: Async](
    dynamoDBClient: DynamoDbAsyncClient,
    appConfig: KinesisPublisherConfig
  )(implicit logger: Logger[F]): Pipe[F, BinlogOffset, BinlogOffset] = {
    val offsets = Table[BinlogOffset](appConfig.checkpointTableName)
    _.evalTap(binLogOffset => logger.info(s"CHECKPOINT offset=$binLogOffset"))
      .evalMap(binLogOffset =>
        ScanamoCats[F](dynamoDBClient)
          .exec(offsets.put(binLogOffset))
          .as(binLogOffset)
      )
  }
}
