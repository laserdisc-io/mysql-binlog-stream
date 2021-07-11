package io.laserdisc.mysql.binlog.kinesis.config
import io.laserdisc.mysql.binlog.config.BinLogConfig
import software.amazon.awssdk.regions.Region

import scala.concurrent.duration.FiniteDuration

case class BinLogKinesisConfig(
      appName: String,
      checkpointEvery: FiniteDuration,
      kinesisOutputStream: String,
      kinesisRegion: Region,
      dynamoDBOffsetTable: String,
      binlogConfig: BinLogConfig,

                    ) {



  // TODO:
//  override def toString: String =
//    s"""AppConfig(env:$env, appName:$appName, kinesisStream:$kinesisStreamName checkpointEvery:$checkpointEvery ddbOffsetTable=$ddbOffsetTable, $binlogConfig)"""
}


