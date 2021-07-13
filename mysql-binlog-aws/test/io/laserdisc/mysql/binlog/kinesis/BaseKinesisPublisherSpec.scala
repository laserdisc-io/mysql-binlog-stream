package io.laserdisc.mysql.binlog.kinesis

import cats.effect.{ Async, IO, Resource }
import cats.implicits._
import com.dimafeng.testcontainers.{ Container, ForAllTestContainer, MultipleContainers }
import db.MySqlContainer
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.util.transactor.Transactor
import fs2.aws.internal.KinesisProducerClient
import io.laserdisc.mysql.binlog.database
import io.laserdisc.mysql.binlog.kinesis.container.DynamoDBContainer
import io.laserdisc.mysql.binlog.stream.Sku
import org.scalatest.wordspec.AnyWordSpec
import org.typelevel.log4cats.Logger
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

import scala.concurrent.duration.DurationInt

abstract class BaseKinesisPublisherSpec
    extends AnyWordSpec
    with ForAllTestContainer
    with DynamoDBContainer
    with MySqlContainer { this: ForAllTestContainer =>

  override val container: Container = MultipleContainers(ddbContainer, singleMySQLContainer)

  override def afterStart(): Unit = createDDBOffsetTestTable()

  def localMySQLTransactor: IO[Resource[IO, HikariTransactor[IO]]] =
    IO.delay(database.transactor[IO](containerBinlogConfig))

  def inserts(txId: Int, offset: Int, itemsPerTx: Int, xa: Transactor[IO]): IO[Unit] =
    (0 until itemsPerTx)
      .map(offset + txId * itemsPerTx + _)
      .map(id => Sku.insert(id, s"sku$id").run)
      .reduce((prev, cur) => prev >> cur)
      .transact(xa)
      .void

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
