package io.laserdisc.mysql.binlog.kinesis

import cats.effect.concurrent.Ref
import cats.effect.{ ContextShift, IO }
import io.circe.generic.auto._
import com.dimafeng.testcontainers.{ Container, ForAllTestContainer, MultipleContainers }
import db.MySqlContainer
import fs2.aws.testkit.TestKinesisProducerClient
import io.laserdisc.mysql.binlog.client.createBinLogClient
import io.laserdisc.mysql.binlog.event.EventMessage
import io.laserdisc.mysql.binlog.kinesis.container.DynamoDBContainer
import io.laserdisc.mysql.binlog.kinesis.utils.BinlogOps.toSingleContainer
import org.scalatest.wordspec.AnyWordSpec
import org.typelevel.log4cats.Logger
import software.amazon.awssdk.regions.Region

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

    val appConfig = KinesisPublisherConfig(
      binlogConfig = containerBinlogConfig,
      checkpointAppName = "dev",
      checkpointEvery = 3.seconds,
      checkpointTableName = "foo-bar-woof",
      checkpointTableRegion = Region.US_EAST_1,
      kinesisOutputStream = "foo-bar-woof-k",
      kinesisRegion = Region.US_EAST_1
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
