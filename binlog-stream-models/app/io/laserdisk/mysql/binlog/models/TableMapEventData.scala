package io.laserdisk.mysql.binlog.models

import com.github.shyiko.mysql.binlog.event.{ TableMapEventData => JTableMapEventData }

object TableMapEventData {
  def unapply(arg: JTableMapEventData): Option[(Long, String)] =
    Some((arg.getTableId, arg.getTable))
}
