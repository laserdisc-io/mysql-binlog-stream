package io.laserdisc.mysql.binlog.kinesis

import io.laserdisc.mysql.binlog.config.BinLogConfig
import software.amazon.awssdk.regions.Region

import scala.concurrent.duration.FiniteDuration

case class KinesisPublisherConfig(
  binlogConfig: BinLogConfig,
  checkpointAppName: String,
  checkpointEvery: FiniteDuration,
  checkpointTableName: String,
  checkpointTableRegion: Region,
  kinesisOutputStream: String,
  kinesisRegion: Region
) {

  // TODO:
//  override def toString: String =
//    s"""AppConfig(env:$env, appName:$appName, kinesisStream:$kinesisStreamName checkpointEvery:$checkpointEvery ddbOffsetTable=$ddbOffsetTable, $binlogConfig)"""
}
