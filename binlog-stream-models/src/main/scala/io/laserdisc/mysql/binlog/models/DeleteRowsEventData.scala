package io.laserdisc.mysql.binlog.models

import com.github.shyiko.mysql.binlog.event.DeleteRowsEventData as JDeleteRowsEventData

import scala.jdk.CollectionConverters.*
object DeleteRowsEventData {
  def unapply(arg: JDeleteRowsEventData): Option[(Long, List[Array[Serializable]], Array[Int])] =
    Some((arg.getTableId, arg.getRows.asScala.toList, arg.getIncludedColumns.stream().toArray))
}
