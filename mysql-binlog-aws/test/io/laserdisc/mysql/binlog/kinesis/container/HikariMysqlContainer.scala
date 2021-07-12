package io.laserdisc.mysql.binlog.kinesis.container

import cats.effect.{IO, Resource}
import cats.implicits._
import com.dimafeng.testcontainers.ForAllTestContainer
import db.MySqlContainer
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.laserdisc.mysql.binlog.database
import io.laserdisc.mysql.binlog.stream.Sku


trait HikariMysqlContainer extends MySqlContainer {
  this: ForAllTestContainer =>

  def mkTransactor: IO[Resource[IO, HikariTransactor[IO]]] =
    IO.delay(database.transactor[IO](containerBinlogConfig))

  def inserts(tx_id: Int, offset: Int, itemsPerTx: Int, xa: Transactor[IO]): IO[Unit] = {

    val v: Seq[doobie.ConnectionIO[Int]] = (0 until itemsPerTx).map { i =>
      val id = offset + tx_id * itemsPerTx + i
      Sku.insert(id, s"sku-$id").run
    }

    v.reduce((prev, cur) => prev >> cur).transact(xa).void
  }

}
