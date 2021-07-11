package io.laserdisc.mysql.binlog.kinesis

import aws.makeMetricsStream
import binlog.binlogStream
import cats.data.Kleisli
import cats.effect.{ ConcurrentEffect, ContextShift, Timer }
import context.BinlogListenerContext
import org.typelevel.log4cats.Logger
import io.laserdisc.mysql.binlog.checkpoint.BinlogOffset

package object main {
  def mainStream[F[_]: ConcurrentEffect: ContextShift: Timer: Logger]: Kleisli[F, BinlogListenerContext[F], fs2.Stream[F, BinlogOffset]] =
    for {
      stream       <- binlogStream[F]
      metricStream <- makeMetricsStream[F]
    } yield stream.concurrently(metricStream)
}
