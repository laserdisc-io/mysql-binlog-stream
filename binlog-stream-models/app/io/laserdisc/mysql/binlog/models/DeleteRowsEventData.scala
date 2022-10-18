package io.laserdisc.mysql.binlog.models

import java.io.Serializable

import com.github.shyiko.mysql.binlog.event.{DeleteRowsEventData => JDeleteRowsEventData}

import scala.jdk.CollectionConverters._
object DeleteRowsEventData {
  def unapply(arg: JDeleteRowsEventData): Option[(Long, List[Array[Serializable]], Array[Int])] =
    Some((arg.getTableId, arg.getRows.asScala.toList, arg.getIncludedColumns.stream().toArray))
}
