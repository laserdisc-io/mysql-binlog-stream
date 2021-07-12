package io.laserdisc.mysql.binlog.kinesis

import cats.effect.concurrent.Ref
import cats.effect.{ ContextShift, IO }
import com.github.shyiko.mysql.binlog.BinaryLogClient
import fs2.aws.internal.KinesisProducerClient
import io.laserdisc.mysql.binlog.client.createBinLogClient
import io.laserdisc.mysql.binlog.stream.TransactionState
import org.typelevel.log4cats.Logger
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

case class BinlogListenerContext[F[_]](
  config: KinesisPublisherConfig,
  dynamoDbAsyncClient: DynamoDbAsyncClient,
  binaryLogClient: BinaryLogClient,
  transactionState: Ref[F, TransactionState],
  kinesisProducer: KinesisProducerClient[F]
) {
  override def toString: String = s"BinlogListenerContext[appConfig=${config.toString}]"
}

object BinlogListenerContext {

  def createdDynamoDbClient(config: KinesisPublisherConfig): IO[DynamoDbAsyncClient] =
    IO.delay(DynamoDbAsyncClient.builder().region(config.checkpointTableRegion).build())

  def mkContext(appConfig: KinesisPublisherConfig)(
    implicit cs: ContextShift[IO],
    logger: Logger[IO]
  ): IO[BinlogListenerContext[IO]] =
    for {
      _                <- logger.info("BINLOGGER-CONNECT-START")
      dynamoClient     <- createdDynamoDbClient(appConfig)
      offset           <- Checkpointing.fetchOffset[IO](dynamoClient, appConfig)
      binlogClient     <- createBinLogClient[IO](appConfig.binlogConfig, offset)
      transactionState <- makeTransactionState(appConfig.binlogConfig, binlogClient)
      kinesisProducer  <- createKinesisProducer[IO](appConfig)

    } yield BinlogListenerContext(
      appConfig,
      dynamoClient,
      binlogClient,
      transactionState,
      kinesisProducer
    )
}
