package io.laserdisc.mysql.binlog.stream

import doobie.implicits._

case class Sku(id: Int, sku: String)

object Sku {
  def insert(id: Int, sku: String): doobie.Update0 =
    sql"""insert into sku (id, sku)
         |values ($id, $sku)""".stripMargin.update

  def truncate =
    sql"""truncate table sku""".stripMargin.update
}
