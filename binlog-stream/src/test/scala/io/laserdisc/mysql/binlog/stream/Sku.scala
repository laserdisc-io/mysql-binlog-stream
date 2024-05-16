package io.laserdisc.mysql.binlog.stream

import doobie.ConnectionIO
import doobie.implicits.*
import doobie.util.update.Update

case class Sku(id: Int, sku: String)

object Sku {
  def insert(id: Int, sku: String): doobie.Update0 =
    sql"""insert into sku (id, sku)
         |values ($id, $sku)""".stripMargin.update

  def insertMany(ps: Sku*): ConnectionIO[Int] = {
    val sql = """insert into sku (id, sku) values (?, ?)""".stripMargin
    Update[Sku](sql).updateMany(ps)
  }

  def truncate =
    sql"""truncate table sku""".stripMargin.update
}
