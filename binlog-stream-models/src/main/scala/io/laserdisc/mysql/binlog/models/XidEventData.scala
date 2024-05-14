package io.laserdisc.mysql.binlog.models

import com.github.shyiko.mysql.binlog.event.XidEventData as JXidEventData

object XidEventData {
  def unapply(arg: JXidEventData): Option[Long] = Some(arg.getXid)
}
