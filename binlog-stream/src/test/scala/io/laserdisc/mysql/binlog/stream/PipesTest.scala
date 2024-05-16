package io.laserdisc.mysql.binlog.stream

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import cats.implicits.*
import com.dimafeng.testcontainers.ForAllTestContainer
import com.github.shyiko.mysql.binlog.BinaryLogClient
import db.MySqlContainerTest
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.laserdisc.mysql.binlog.database
import io.laserdisc.mysql.binlog.models.SchemaMetadata
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.laserdisc.mysql.binlog.config.BinLogConfigOps

import scala.util.control

class PipesTest extends AnyWordSpec with Matchers with ForAllTestContainer with MySqlContainerTest {

  "Binlog Events Stream" should {
    "handle truncate table as separate transaction" in {

      val testTransactor: Resource[IO, HikariTransactor[IO]] = database.transactor[IO](binlogConfig)

      val client: BinaryLogClient = binlogConfig.mkBinaryLogClient()

      client.registerLifecycleListener(new BinaryLogClient.AbstractLifecycleListener {
        override def onConnect(client: BinaryLogClient): Unit = {
          control.Exception.allCatch.opt {
            testTransactor
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
          ()
        }
      })

      val events = testTransactor
        .use { implicit xa =>
          implicit val logger: Logger[IO] = Slf4jLogger.getLoggerFromName[IO]("testing")
          for {
            schemaMetadata <- SchemaMetadata.buildSchemaMetadata("test")
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
