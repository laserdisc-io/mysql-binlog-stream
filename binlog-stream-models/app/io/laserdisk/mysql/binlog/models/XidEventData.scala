package io.laserdisk.mysql.binlog.models

import com.github.shyiko.mysql.binlog.event.{ XidEventData => JXidEventData }

object XidEventData {
  def unapply(arg: JXidEventData): Option[Long] = Some(arg.getXid)
}
