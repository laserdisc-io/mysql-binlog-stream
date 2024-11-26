package io.laserdisc.mysql.binlog.models

import com.github.shyiko.mysql.binlog.event.{RotateEventData => JRotateEventData}

object RotateEventData {
  def unapply(arg: JRotateEventData): Option[(String, Long)] =
    Some((arg.getBinlogFilename, arg.getBinlogPosition))
}
