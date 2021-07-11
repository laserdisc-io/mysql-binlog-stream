package io.laserdisc.mysql.binlog.kinesis.binlog
import cats.effect.{ Async, Sync }
import config.BinLogKinesisConfig
import fs2.Pipe
import cats.implicits._
import org.typelevel.log4cats.Logger
import io.laserdisc.mysql.binlog.checkpoint.BinlogOffset
import org.scanamo.syntax._
import org.scanamo.generic.auto._
import org.scanamo.{ DynamoReadError, ScanamoCats, Table }
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

object Checkpointing {

  def fetchOffset[F[_]: Async](
    dynamoDBClient: DynamoDbAsyncClient,
    appConfig: BinLogKinesisConfig
  ): F[Option[BinlogOffset]] = {
    val offsets = Table[BinlogOffset](appConfig.ddbOffsetTable)
    println(s"LOADING OFFSETS FROM $offsets")
    ScanamoCats[F](dynamoDBClient)
      .exec(offsets.get("appName" === appConfig.appName))
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
    appConfig: BinLogKinesisConfig
  )(implicit logger: Logger[F]): Pipe[F, BinlogOffset, BinlogOffset] = {
    val offsets = Table[BinlogOffset](appConfig.ddbOffsetTable)
    _.evalTap(binLogOffset => logger.info(s"CHECKPOINT offset=$binLogOffset"))
      .evalMap(binLogOffset =>
        ScanamoCats[F](dynamoDBClient)
          .exec(offsets.put(binLogOffset))
          .as(binLogOffset)
      )
  }
}
