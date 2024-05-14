package io.laserdisc.mysql.binlog.checkpoint

import io.laserdisc.mysql.binlog.event.Offset

case class BinlogOffset(appName: String, override val fileName: String, override val offset: Long) extends Offset

object BinlogOffset {

  def compareOffsets(one: BinlogOffset, two: BinlogOffset): BinlogOffset =
    if (one > two) one else two
}
