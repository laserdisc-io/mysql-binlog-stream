package io.laserdisc.mysql.binlog.models

import java.io

import com.github.shyiko.mysql.binlog.event.{UpdateRowsEventData => JUpdateRowsEventData}

import scala.jdk.CollectionConverters._

object UpdateRowsEventData {
  def unapply(
      arg: JUpdateRowsEventData
  ): Some[(Long, List[(Array[io.Serializable], Array[io.Serializable])], Array[Int])] = {
    val beforeAfter =
      arg.getRows.asScala.toList.map(entry => entry.getKey -> entry.getValue)
    Some((arg.getTableId, beforeAfter, arg.getIncludedColumns.stream().toArray))
  }
}
