package io.laserdisc.mysql.binlog.kinesis

//import cats.implicits._
import cats.effect.Async
import com.dimafeng.testcontainers.{ Container, ForAllTestContainer, MultipleContainers }
import fs2.aws.internal.KinesisProducerClient
import io.laserdisc.mysql.binlog.kinesis.container.{ DynamoDBContainer, HikariMysqlContainer }
import org.scalatest.wordspec.AnyWordSpec
import org.typelevel.log4cats.Logger
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

import scala.concurrent.duration.DurationInt

abstract class BinLogDockerSpec
    extends AnyWordSpec
    with ForAllTestContainer
    with DynamoDBContainer
    with HikariMysqlContainer {

  override val container: Container = MultipleContainers(ddbContainer, singleMySQLContainer)

  override def afterStart(): Unit =
    createDDBOffsetTestTable()

  def mkTestConfig[F[_]: Async](kpc: KinesisProducerClient[F]): PublisherConfig[F] =
    new PublisherConfig[F](
      binlogConfig = containerBinlogConfig,
      checkpointAppName = "testApp",
      checkpointEvery = 3.seconds,
      checkpointTableName = TestOffsetTableName,
      checkpointTableRegion = Region.US_EAST_1,
      kinesisOutputStream = "some-test-stream",
      kinesisRegion = Region.US_EAST_1
    ) {

      override def createDynamoDBClient: F[DynamoDbAsyncClient] = Async[F].delay(containerDDBClient)

      override def createKinesisProducer(implicit logger: Logger[F]): F[KinesisProducerClient[F]] =
        Async[F].delay(kpc)
    }

}
