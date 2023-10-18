package io.laserdisc.mysql.binlog.models

import com.github.shyiko.mysql.binlog.event.{TableMapEventData => JTableMapEventData}

object TableMapEventData {
  def unapply(arg: JTableMapEventData): Option[(Long, String, String)] =
    Some((arg.getTableId, arg.getDatabase, arg.getTable))
}
