package io.laserdisc.mysql.binlog.stream

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import com.dimafeng.testcontainers.ForAllTestContainer
import com.github.shyiko.mysql.binlog.BinaryLogClient
import com.github.shyiko.mysql.binlog.event.{EventHeaderV4, EventType}
import db.MySqlContainerTest
import doobie.hikari.HikariTransactor
import doobie.implicits._
import io.laserdisc.mysql.binlog.config.BinLogConfigOps
import io.laserdisc.mysql.binlog.database
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class MysqlBinlogStreamTest extends AnyWordSpec with ForAllTestContainer with MySqlContainerTest with Matchers {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  "Binlog stream" should {

    "emit events from mysql" in {

      val testTransactor: Resource[IO, HikariTransactor[IO]] = database.transactor[IO](binlogConfig)

      val client: BinaryLogClient = binlogConfig.mkBinaryLogClient()

      client.registerLifecycleListener(new BinaryLogClient.AbstractLifecycleListener {
        override def onConnect(client: BinaryLogClient): Unit =
          testTransactor
            .use(xa =>
              Sku
                .insertMany(
                  Sku(2, "two"),
                  Sku(3, "three"),
                  Sku(4, "four")
                )
                .transact(xa)
            )
            .map(_ => ())
            .unsafeRunSync()
      })

      val updates = MysqlBinlogStream
        .rawEvents[IO](client)
        .takeWhile(_.getHeader[EventHeaderV4]().getEventType != EventType.XID)         // COMMIT
        .filter(_.getHeader[EventHeaderV4]().getEventType == EventType.EXT_WRITE_ROWS) // INSERT
        .compile
        .toList
        .unsafeRunSync()

      updates should have size 3
    }
  }
}
