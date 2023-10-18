package io.laserdisc.mysql.binlog.stream

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import cats.implicits._
import com.dimafeng.testcontainers.ForAllTestContainer
import com.github.shyiko.mysql.binlog.BinaryLogClient
import db.MySqlContainerTest
import doobie.hikari.HikariTransactor
import doobie.implicits._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.laserdisc.mysql.binlog.database
import io.laserdisc.mysql.binlog.models.SchemaMetadata
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.language.reflectiveCalls
import scala.util.control

class PipesTest extends AnyWordSpec with Matchers with ForAllTestContainer with MySqlContainerTest {

  def fixture =
    new {

      val cfg = containerBinlogConfig

      val testTransactor: Resource[IO, HikariTransactor[IO]] = database.transactor[IO](cfg)

      val client = cfg.mkBinaryLogClient()
    }

  "Binlog Events Stream" should {
    "handle truncate table as separate transaction" in {
      val (client, xaResource) = (fixture.client, fixture.testTransactor)

      client.registerLifecycleListener(new BinaryLogClient.AbstractLifecycleListener {
        override def onConnect(client: BinaryLogClient): Unit =
          control.Exception.allCatch.opt {
            xaResource
              .use { xa =>
                for {
                  _ <- (Sku.truncate.run *> Sku.insert(5, "sku5").run *>
                    Sku.insert(2, "sku2").run).transact(xa)

                  _ <- (Sku.insert(6, "sku6").run *> Sku.truncate.run *>
                    Sku.insert(2, "sku2").run).transact(xa)

                  _ <- (Sku.insert(5, "sku5").run *>
                    Sku.insert(7, "sku7").run *> Sku.truncate.run)
                    .transact(xa)

                  _ <- Sku.truncate.run.transact(xa)
                } yield ()
              }
              .unsafeRunSync()
          }
      })

      val events = xaResource
        .use { implicit xa =>
          for {
            implicit0(logger: Logger[IO]) <- Slf4jLogger.fromName[IO]("testing")
            schemaMetadata                <- SchemaMetadata.buildSchemaMetadata("test")
            transactionState <- TransactionState
              .createTransactionState[IO](schemaMetadata, client)
            actions <- MysqlBinlogStream
              .rawEvents[IO](client)
              .through(streamEvents(transactionState, "test"))
              .map(_.action)
              .take(10)
              .compile
              .toList
          } yield actions
        }
        .unsafeRunSync()

      events should be(
        List(
          "truncate",
          "create",
          "create",
          "create",
          "truncate",
          "create",
          "create",
          "create",
          "truncate",
          "truncate"
        )
      )
    }
  }

}
