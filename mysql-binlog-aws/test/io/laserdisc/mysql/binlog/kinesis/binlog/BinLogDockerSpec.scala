package io.laserdisc.mysql.binlog.kinesis.binlog

import binlog.container.{ DynamoDBContainer, MySqlContainer }
import cats.effect.concurrent.Ref
import cats.effect.{ ContextShift, IO }
import com.dimafeng.testcontainers.{ Container, ForAllTestContainer, MultipleContainers }
import config.BinLogKinesisConfig
import config.SDLCEnvironment.SDLCEnvironment.dev
import context.BinlogListenerContext
import fs2.aws.testkit.TestKinesisProducerClient
import org.typelevel.log4cats.Logger
import io.circe.generic.auto._
import io.laserdisc.mysql.binlog.client.createBinLogClient
import io.laserdisc.mysql.binlog.event.EventMessage
import org.scalatest.wordspec.AnyWordSpec
import utils.BinlogOps.toSingleContainer

import scala.concurrent.duration.DurationInt

abstract class BinLogDockerSpec
    extends AnyWordSpec
    with ForAllTestContainer
    with DynamoDBContainer
    with MySqlContainer {

  override val container: Container =
    MultipleContainers(ddbContainer, toSingleContainer(mySqlContainer))

  override def afterStart(): Unit =
    createDDBOffsetTestTable()

  def mkTestContext(produced: Ref[IO, List[EventMessage]])(
    implicit cs: ContextShift[IO],
    logger: Logger[IO]
  ): IO[BinlogListenerContext[IO]] = {

    val appConfig = AppConfig(
      appName = "dev",
      env = dev,
      checkpointEvery = 3.seconds,
      binlogConfig = containerBinlogConfig
    )

    for {
      dynamoClient     <- containerDDBClient
      offset           <- Checkpointing.fetchOffset[IO](dynamoClient, appConfig)
      binlogClient     <- createBinLogClient[IO](containerBinlogConfig, offset)
      transactionState <- makeTransactionState(containerBinlogConfig, binlogClient)

    } yield BinlogListenerContext(
      appConfig,
      dynamoClient,
      binlogClient,
      transactionState,
      TestKinesisProducerClient[IO, EventMessage](produced)
    )
  }

}
