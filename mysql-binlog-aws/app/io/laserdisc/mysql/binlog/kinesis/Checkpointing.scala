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

  def getOffsetCheckpoint[F[_]: Async](
    dynamoDBClient: DynamoDbAsyncClient,
    config: PublisherConfig[F]
  ): F[Option[BinlogOffset]] = {
    val offsets = Table[BinlogOffset](config.checkpointTableName)
    ScanamoCats[F](dynamoDBClient)
      .exec(offsets.get("appName" === config.checkpointAppName))
      .flatMap {
        case Some(value) =>
          value
            .leftMap(e => new RuntimeException(DynamoReadError.describe(e)))
            .liftTo[F]
            .map(Some(_))
        case None => Sync[F].pure(None)
      }
  }

  def saveOffsetCheckpoint[F[_]: Async](
    dynamoDBClient: DynamoDbAsyncClient,
    config: PublisherConfig[F]
  )(implicit logger: Logger[F]): Pipe[F, BinlogOffset, BinlogOffset] = {
    val offsetTbl = Table[BinlogOffset](config.checkpointTableName)
    _.evalTap(binLogOffset =>
      logger.info(
        s"CHECKPOINT-SAVE offset=$binLogOffset table:${offsetTbl.name} appName:${config.checkpointAppName}"
      )
    ).evalMap(o => ScanamoCats[F](dynamoDBClient).exec(offsetTbl.put(o)).as(o))
  }

}
