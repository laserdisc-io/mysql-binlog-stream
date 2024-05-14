package io.laserdisc.mysql.binlog.event

import io.circe.Json

case class EventMessage(
    table: String,
    timestamp: Long,
    action: String,
    xaId: Option[Long],
    override val fileName: String,
    override val offset: Long,
    endOfTransaction: Boolean,
    pk: Json,
    row: Json
) extends Offset
