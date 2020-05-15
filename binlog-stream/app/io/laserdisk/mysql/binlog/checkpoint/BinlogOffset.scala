package io.laserdisk.mysql.binlog.checkpoint

case class BinlogOffset(appName: String, fileName: String, offset: Long)

object BinlogOffset {
  def compareOffsets(one: BinlogOffset, two: BinlogOffset): BinlogOffset =
    if (f"${one.fileName}${one.offset}%015d".compareTo(f"${two.fileName}${two.offset}%015d") < 0)
      two
    else one
}
