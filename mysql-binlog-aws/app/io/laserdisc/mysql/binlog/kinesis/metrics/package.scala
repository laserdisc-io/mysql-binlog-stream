//package io.laserdisc.mysql.binlog.kinesis
//
//import cats.data.Kleisli
//import cats.effect.concurrent.Ref
//import cats.effect.{ Async, ContextShift, Timer }
//import cats.implicits._
//import org.typelevel.log4cats.Logger
//import io.laserdisc.mysql.binlog.database
//import io.laserdisc.mysql.binlog.models.BinaryLogs
//
//import scala.concurrent.duration._
//
//package object metrics {
//
//  def makeMetricsStream[
//    F[_]: Async: ContextShift: Timer: Logger
//  ]: Kleisli[F, PublisherContext[F], fs2.Stream[F, Unit]] =
//    Kleisli[F, PublisherContext[F], fs2.Stream[F, Unit]] { context =>
//      val env = context.config.env.entryName
//      for {
//        behindMetric   <- Ref[F].of(Metric("binlog-listener", "behind-head", env, 0L))
//        behindMBMetric <- Ref[F].of(Metric("binlog-listener", "behind-head-MB", env, 0L))
//        stream = fs2.Stream.awakeEvery[F](30 seconds).evalMap { _ =>
//                   for {
//                     binlogTxnState <- context.transactionState.get
//                     binlogBytesBehind <- database
//                                            .transactor[F](context.config.binlogConfig)
//                                            .use(implicit xa =>
//                                              BinaryLogs.bytesBehindTheHead[F](
//                                                binlogTxnState.fileName,
//                                                binlogTxnState.offset
//                                              )
//                                            )
//
//                     behindHeadCnt = (System.currentTimeMillis() - binlogTxnState.timestamp) / 60000
//                     behindHeadMB  = binlogBytesBehind / 1000000
//
//                     published1 <- behindMetric.modifyState(publish(behindHeadCnt))
//                     published2 <- behindMBMetric.modifyState(publish(behindHeadMB))
//
//                     _ <- Logger[F].info(s"METRICS-PUBLISHED $published1, $published2")
//
//                   } yield ()
//                 }
//      } yield stream
//    }
//}
