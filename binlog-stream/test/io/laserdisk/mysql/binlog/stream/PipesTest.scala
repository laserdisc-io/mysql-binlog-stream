package io.laserdisk.mysql.binlog.stream

import java.net.URI

import cats.effect.{ IO, Resource }
import cats.implicits._
import com.dimafeng.testcontainers.ForAllTestContainer
import com.github.shyiko.mysql.binlog.BinaryLogClient
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer.CompatibilityMode
import db.MySqlContainer
import doobie.hikari.HikariTransactor
import doobie.implicits._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.laserdisk.mysql.binlog.database
import io.laserdisk.mysql.binlog.database.DbConfig
import io.laserdisk.mysql.binlog.models.SchemaMetadata
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.language.reflectiveCalls
import scala.util.control

class PipesTest extends AnyWordSpec with Matchers with ForAllTestContainer with MySqlContainer {
  def fixture =
    new {
      val testTransactor: Resource[IO, HikariTransactor[IO]] = database.transactor(
        DbConfig(
          mySqlContainer.getUsername,
          mySqlContainer.getPassword,
          s"${mySqlContainer.getJdbcUrl}?useSSL=false",
          3
        )
      )
      val cleanURI = mySqlContainer.getJdbcUrl.substring(5)
      val uri      = URI.create(cleanURI)
      val client = {
        val c = new BinaryLogClient(
          uri.getHost,
          uri.getPort,
          mySqlContainer.getUsername,
          mySqlContainer.getPassword
        )
        val eventDeserializer = new EventDeserializer()
        eventDeserializer.setCompatibilityMode(
          CompatibilityMode.DATE_AND_TIME_AS_LONG_MICRO,
          CompatibilityMode.CHAR_AND_BINARY_AS_BYTE_ARRAY
        )
        c.setEventDeserializer(eventDeserializer)
        c
      }
    }
  "Binlog Events Stream" should {
    "handle truncate table as separate transaction" in {
      implicit val (client, xaResource) = (fixture.client, fixture.testTransactor)
      client.registerLifecycleListener(new BinaryLogClient.AbstractLifecycleListener {
        override def onConnect(client: BinaryLogClient): Unit =
          control.Exception.allCatch.opt {
            xaResource
              .use {
                xa =>
                  for {
                    _ <- (Sku.truncate.run *> Sku.insert(5, "sku5").run *>
                          Sku.insert(2, "sku2").run).transact(xa)

                    _ <- (Sku.insert(6, "sku6").run *> Sku.truncate.run *>
                          Sku.insert(2, "sku2").run).transact(xa)

                    _ <- (Sku.insert(5, "sku5").run *>
                          Sku.insert(7, "sku7").run *> Sku.truncate.run).transact(xa)

                    _ <- Sku.truncate.run.transact(xa)
                  } yield ()
              }
              .unsafeRunSync()
          }
      })

      val events = xaResource
        .use { implicit xa =>
          for {
            implicit0(logger: SelfAwareStructuredLogger[IO]) <- Slf4jLogger.fromName[IO]("testing")
            schemaMetadata                                   <- SchemaMetadata.buildSchemaMetadata("test")
            transactionState                                 <- TransactionState.createTransactionState[IO](schemaMetadata, client)
            actions <- MysqlBinlogStream
                        .rawEvents[IO](client)
                        .through(streamEvents(transactionState))
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
