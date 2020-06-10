package io.laserdisc.mysql.binlog.event

import io.circe.Json

case class EventMessage(
  table: String,
  timestamp: Long,
  action: String,
  xaId: Option[Long],
  fileName: String,
  offset: Long,
  endOfTransaction: Boolean,
  pk: Json,
  row: Json
)
