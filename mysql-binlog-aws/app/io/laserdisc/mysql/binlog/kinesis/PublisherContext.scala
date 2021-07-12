package io.laserdisc.mysql.binlog.kinesis

import cats.effect.concurrent.Ref
import cats.effect.{ Async, ContextShift }
import cats.implicits._
import com.github.shyiko.mysql.binlog.BinaryLogClient
import fs2.aws.internal.KinesisProducerClient
import io.laserdisc.mysql.binlog.client.createBinLogClient
import io.laserdisc.mysql.binlog.config.BinLogConfig
import io.laserdisc.mysql.binlog.database
import io.laserdisc.mysql.binlog.models.SchemaMetadata
import io.laserdisc.mysql.binlog.stream.TransactionState
import org.typelevel.log4cats.Logger
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

case class PublisherContext[F[_]](
  config: PublisherConfig[F],
  dynamoDBClient: DynamoDbAsyncClient,
  kinesisProducer: KinesisProducerClient[F],
  binaryLogClient: BinaryLogClient,
  transactionState: Ref[F, TransactionState]
) {
  override def toString: String = s"PublisherContext[config=${config.toString}]"
}

object PublisherContext {

  def apply[F[_]: Async](appConfig: PublisherConfig[F])(
    implicit cs: ContextShift[F],
    logger: Logger[F]
  ): F[PublisherContext[F]] =
    for {
      _        <- logger.info("PUBLISHER-INIT-START")
      ddb      <- appConfig.createDynamoDBClient
      kinesis  <- appConfig.createKinesisProducer
      offset   <- Checkpointing.getOffsetCheckpoint[F](ddb, appConfig)
      binlog   <- createBinLogClient[F](appConfig.binlogConfig, offset)
      txnState <- makeTransactionState(appConfig.binlogConfig, binlog)
      _        <- logger.info("PUBLISHER-INIT-COMPLETE")
    } yield PublisherContext[F](appConfig, ddb, kinesis, binlog, txnState)

  def makeTransactionState[F[_]: Async](
    config: BinLogConfig,
    binlogClient: BinaryLogClient
  )(
    implicit cs: ContextShift[F],
    logger: Logger[F]
  ): F[Ref[F, TransactionState]] =
    database
      .transactor[F](config)
      .use(implicit xa => SchemaMetadata.buildSchemaMetadata(config.schema))
      .flatMap(sm => TransactionState.createTransactionState[F](sm, binlogClient))

}
