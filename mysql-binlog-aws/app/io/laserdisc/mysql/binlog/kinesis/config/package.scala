package io.laserdisc.mysql.binlog.kinesis

import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Encoder, Json}
import io.laserdisc.mysql.binlog.config.BinLogConfig

import scala.concurrent.duration.FiniteDuration

package object config {

  implicit val appConfigEnc: Encoder[BinLogKinesisConfig] = deriveEncoder

  implicit val binlogConfigEnc: Encoder[BinLogConfig] = deriveEncoder

  implicit final val finiteDurationEncoder: Encoder[FiniteDuration] = (fd: FiniteDuration) => Json.fromString(fd.toString())

}
