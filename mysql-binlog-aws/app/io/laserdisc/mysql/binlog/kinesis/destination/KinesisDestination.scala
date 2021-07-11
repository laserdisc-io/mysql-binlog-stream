package io.laserdisc.mysql.binlog.kinesis.destination
import io.laserdisc.mysql.binlog.event.EventMessage
import io.laserdisc.mysql.binlog.kinesis.json

object KinesisDestination {
  def calculateKey(payload: EventMessage): String =
    s"${payload.table}|${json.flatSort(payload.pk).mkString("|")}"
}
