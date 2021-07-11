package io.laserdisc.mysql.binlog.kinesis.models

import doobie.implicits._

case class TestTableModel(id: Int, name: String)

object TestTableModel {

  def performInsert(id: Int, name: String): doobie.Update0 =
    sql"""insert into test_table (id, name)
         |values ($id, $name)""".stripMargin.update

  def performTruncate: doobie.Update0 =
    sql"""truncate table test_table""".stripMargin.update
}
