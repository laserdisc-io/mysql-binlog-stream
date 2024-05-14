package io.laserdisc.mysql.binlog.models

import com.github.shyiko.mysql.binlog.event.WriteRowsEventData as JWriteRowsEventData

import scala.jdk.CollectionConverters.*

object WriteRowsEventData {
  def unapply(arg: JWriteRowsEventData): Option[(Long, List[Array[Serializable]], Array[Int])] =
    Some((arg.getTableId, arg.getRows.asScala.toList, arg.getIncludedColumns.stream().toArray))
}
