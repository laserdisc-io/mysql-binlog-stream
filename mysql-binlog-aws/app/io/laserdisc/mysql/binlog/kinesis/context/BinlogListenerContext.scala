package io.laserdisc.mysql.binlog.kinesis.context

import binlog.{ makeTransactionState, Checkpointing }
import cats.effect.concurrent.Ref
import cats.effect.{ ContextShift, IO }
import com.github.shyiko.mysql.binlog.BinaryLogClient
import config.BinLogKinesisConfig
import fs2.aws.internal.KinesisProducerClient
import org.typelevel.log4cats.Logger
import io.laserdisc.mysql.binlog.client.createBinLogClient
import io.laserdisc.mysql.binlog.stream.TransactionState
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

case class BinlogListenerContext[F[_]](
                                        appConfig: BinLogKinesisConfig,
                                        dynamoDbAsyncClient: DynamoDbAsyncClient,
                                        binaryLogClient: BinaryLogClient,
                                        transactionState: Ref[F, TransactionState],
                                        kinesisProducer: KinesisProducerClient[F]
) {
  override def toString: String = s"BinlogListenerContext[appConfig=${appConfig.toString}]"
}

object BinlogListenerContext {

  def createdDynamoDbClient: IO[DynamoDbAsyncClient] =
    IO.delay(DynamoDbAsyncClient.builder().region(BinLogKinesisConfig.region).build())

  def mkContext(appConfig: BinLogKinesisConfig)(
    implicit cs: ContextShift[IO],
    logger: Logger[IO]
  ): IO[BinlogListenerContext[IO]] =
    for {
      _                <- logger.info("BINLOGGER-CONNECT-START")
      dynamoClient     <- createdDynamoDbClient
      offset           <- Checkpointing.fetchOffset[IO](dynamoClient, appConfig)
      binlogClient     <- createBinLogClient[IO](appConfig.binlogConfig, offset)
      transactionState <- makeTransactionState(appConfig.binlogConfig, binlogClient)
      kinesisProducer  <- binlog.createKinesisProducer[IO]

    } yield BinlogListenerContext(
      appConfig,
      dynamoClient,
      binlogClient,
      transactionState,
      kinesisProducer
    )
}
