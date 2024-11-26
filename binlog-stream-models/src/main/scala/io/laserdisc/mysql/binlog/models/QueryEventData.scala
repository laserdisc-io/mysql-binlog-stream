package io.laserdisc.mysql.binlog.models

import com.github.shyiko.mysql.binlog.event.{QueryEventData => JQueryEventData}

object QueryEventData {
  def truncateTable(sql: String) = {
    val regex = "truncate( table)? (.+)".r
    sql match {
      case regex(_, table) => Some(table)
      case _               => None
    }
  }
  def unapply(arg: JQueryEventData): Option[(String, String, String, Option[String])] =
    truncateTable(arg.getSql.toLowerCase) match {
      case Some(table) =>
        Some((arg.getSql.toLowerCase, arg.getDatabase, "truncate", Some(table)))
      case None =>
        Some((arg.getSql.toLowerCase, arg.getDatabase, "query", None))
    }
}
