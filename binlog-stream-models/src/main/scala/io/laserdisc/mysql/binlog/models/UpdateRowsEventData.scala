package io.laserdisc.mysql.binlog.models

import com.github.shyiko.mysql.binlog.event.{UpdateRowsEventData => JUpdateRowsEventData}

import scala.jdk.CollectionConverters._

object UpdateRowsEventData {
  def unapply(
      arg: JUpdateRowsEventData
  ): Some[(Long, List[(Array[Serializable], Array[Serializable])], Array[Int])] = {
    val beforeAfter =
      arg.getRows.asScala.toList.map(entry => entry.getKey -> entry.getValue)
    Some((arg.getTableId, beforeAfter, arg.getIncludedColumns.stream().toArray))
  }
}
