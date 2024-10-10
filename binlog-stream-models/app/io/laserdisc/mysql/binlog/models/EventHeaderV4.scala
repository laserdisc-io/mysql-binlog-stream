package io.laserdisc.mysql.binlog.models

import com.github.shyiko.mysql.binlog.event.{ EventHeaderV4 => JEventHeaderV4, EventType }

object EventHeaderV4 {
  def unapply(arg: JEventHeaderV4): Option[(EventType, Long, Long)] =
    Some((arg.getEventType, arg.getTimestamp, arg.getNextPosition))
}
