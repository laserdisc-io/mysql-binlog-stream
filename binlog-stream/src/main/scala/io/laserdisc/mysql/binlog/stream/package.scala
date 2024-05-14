package io.laserdisc.mysql.binlog

import cats.effect.kernel.Ref
import cats.effect.{Concurrent, Sync}
import cats.implicits.*
import com.github.shyiko.mysql.binlog.event.Event
import org.typelevel.log4cats.Logger
import io.laserdisc.mysql.binlog.event.EventMessage

package object stream {
  def streamEvents[F[_]: Concurrent: Sync: Logger](
      transactionState: Ref[F, TransactionState],
      schema: String
  ): fs2.Pipe[F, Event, EventMessage] =
    _.through(streamTransactionPackages[F](transactionState, schema)).flatMap(pkg =>
      fs2.Stream.eval(warnBigTransactionPackage(pkg)) >> fs2.Stream(pkg.events*)
    )

  def streamCompactedEvents[F[_]: Concurrent: Logger](
      transactionState: Ref[F, TransactionState],
      schema: String
  ): fs2.Pipe[F, Event, EventMessage] =
    _.through(streamTransactionPackages[F](transactionState, schema)).flatMap(pkg => fs2.Stream(compaction.compact(pkg.events)*))

  def streamTransactionPackages[F[_]: Concurrent: Logger](
      transactionState: Ref[F, TransactionState],
      schema: String
  ): fs2.Pipe[F, Event, TransactionPackage] =
    _.evalMap(event =>
      Logger[F].debug(s"received binlog event $event") >> transactionState.modifyState(
        TransactionState.nextState(event, schema)
      )
    ).unNone

  def warnBigTransactionPackage[F[_]: Sync: Logger](
      transactionPackage: TransactionPackage
  ): F[Unit] =
    if (transactionPackage.events.size >= 1000)
      for {
        distro <- Sync[F].delay(transactionPackage.events.groupBy(_.table).map { case (k, v) =>
          k -> v.size
        })
        _ <- Logger[F].warn(s"""Transaction has > then 1000 elements in it with
                                 |following distribution $distro
        """.stripMargin)
      } yield ()
    else
      Sync[F].unit

}
