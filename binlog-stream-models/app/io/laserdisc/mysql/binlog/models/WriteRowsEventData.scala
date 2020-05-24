package io.laserdisc.mysql.binlog.models

import java.io.Serializable

import com.github.shyiko.mysql.binlog.event.{ WriteRowsEventData => JWriteRowsEventData }

import scala.jdk.CollectionConverters._

object WriteRowsEventData {
  def unapply(arg: JWriteRowsEventData): Option[(Long, List[Array[Serializable]], Array[Int])] =
    Some((arg.getTableId, arg.getRows.asScala.toList, arg.getIncludedColumns.stream().toArray))
}
