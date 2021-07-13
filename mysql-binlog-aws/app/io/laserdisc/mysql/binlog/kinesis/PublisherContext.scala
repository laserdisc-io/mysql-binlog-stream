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

  def apply[F[_]: Async](config: PublisherConfig[F])(
    implicit cs: ContextShift[F],
    logger: Logger[F]
  ): F[PublisherContext[F]] =
    for {
      _        <- logger.info("PUBLISHER-INIT-START")
      ddb      <- config.createDynamoDBClient
      kinesis  <- config.createKinesisProducer
      offset   <- Checkpointing.getOffsetCheckpoint[F](ddb, config)
      binlog   <- createBinLogClient[F](config.binlogConfig, offset)
      txnState <- makeTransactionState(config.binlogConfig, binlog)
      _        <- logger.info("PUBLISHER-INIT-COMPLETE")
    } yield PublisherContext[F](config, ddb, kinesis, binlog, txnState)

  private[this] def makeTransactionState[F[_]: Async](
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
