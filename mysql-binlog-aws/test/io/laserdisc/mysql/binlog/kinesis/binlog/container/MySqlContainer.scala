package io.laserdisc.mysql.binlog.kinesis.binlog.container

import cats.effect.{ContextShift, IO, Resource}
import cats.implicits._
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.laserdisc.mysql.binlog.config.BinLogConfig
import io.laserdisc.mysql.binlog.database
import models.TestTableModel
import org.testcontainers.containers.MySQLContainer

import java.net.URI
import scala.concurrent.ExecutionContext

trait MySqlContainer {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  type OTCContainer = MySQLContainer[T] forSome {
    type T <: MySQLContainer[T]
  }

  val mySqlContainer: OTCContainer = new MySQLContainer("mysql:5.7").withUsername("root")
  mySqlContainer.withCommand("mysqld --log-bin --server-id=1 --binlog-format=ROW")
  mySqlContainer.withInitScript("init.sql")
  mySqlContainer.withPassword("")

  def containerBinlogConfig: BinLogConfig = {

    val cleanURI = mySqlContainer.getJdbcUrl.substring(5)
    val uri      = URI.create(cleanURI)

    BinLogConfig(
      uri.getHost,
      uri.getPort,
      mySqlContainer.getUsername,
      mySqlContainer.getPassword,
      mySqlContainer.getDatabaseName,
      useSSL = false,
      poolSize = 3
    )
  }

  val mkTransactor: IO[Resource[IO, HikariTransactor[IO]]] =
    IO.delay(database.transactor[IO](containerBinlogConfig))

  def inserts(tx_id: Int, offset: Int, itemsPerTx: Int, xa: Transactor[IO]): IO[Unit] = {

    val v: Seq[doobie.ConnectionIO[Int]] = (0 until itemsPerTx).map { i =>
      val id = offset + tx_id * itemsPerTx + i
      TestTableModel.performInsert(id, s"inserted record #$id").run
    }

    v.reduce((prev, cur) => prev >> cur).transact(xa).void
  }

}
