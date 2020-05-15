package io.laserdisk.mysql.binlog.models
import com.github.shyiko.mysql.binlog.event.{ EventType, EventHeaderV4 => JEventHeaderV4 }

object EventHeaderV4 {
  def unapply(arg: JEventHeaderV4): Option[(EventType, Long, Long)] =
    Some((arg.getEventType, arg.getTimestamp, arg.getNextPosition))
}
