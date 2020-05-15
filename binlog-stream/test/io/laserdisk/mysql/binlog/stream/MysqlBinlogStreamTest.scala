package io.laserdisk.mysql.binlog.stream

import java.net.URI

import cats.effect.{ IO, Resource }
import cats.implicits._
import com.dimafeng.testcontainers.ForAllTestContainer
import com.github.shyiko.mysql.binlog.BinaryLogClient
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer.CompatibilityMode
import com.github.shyiko.mysql.binlog.event.{ EventHeaderV4, EventType }
import db.MySqlContainer
import doobie.hikari.HikariTransactor
import doobie.implicits._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import cats.implicits._
import io.laserdisk.mysql.binlog.database
import io.laserdisk.mysql.binlog.database.DbConfig
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.language.reflectiveCalls

class MysqlBinlogStreamTest
    extends AnyWordSpec
    with ForAllTestContainer
    with MySqlContainer
    with Matchers {

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
        val v = new BinaryLogClient(
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
        v.setEventDeserializer(eventDeserializer)
        v
      }
    }

  "Binlog stream" should {

    "emit events from mysql" in {

      val (client, xaResource) = (fixture.client, fixture.testTransactor)
      client.registerLifecycleListener(new BinaryLogClient.AbstractLifecycleListener {
        override def onConnect(client: BinaryLogClient): Unit =
          xaResource
            .use(
              xa =>
                (Sku.insert(2, "sku2").run >>
                  Sku.insert(3, "sku3").run >>
                  Sku.insert(4, "sku4").run).transact(xa)
            )
            .unsafeRunSync()
      })

      val s = for {
        implicit0(logger: SelfAwareStructuredLogger[IO]) <- fs2.Stream.eval(
                                                             Slf4jLogger.fromName[IO]("application")
                                                           )
        event <- MysqlBinlogStream.rawEvents[IO](client)
        _     <- fs2.Stream.eval(logger.info(s"event received $event"))
      } yield event

      val updates = s
        .takeWhile(e => e.getHeader[EventHeaderV4]().getEventType != EventType.XID)
        .filter(e => e.getHeader[EventHeaderV4]().getEventType == EventType.EXT_WRITE_ROWS)
        .compile
        .toList
        .unsafeRunSync()
      updates should have size 3
    }
  }
}
