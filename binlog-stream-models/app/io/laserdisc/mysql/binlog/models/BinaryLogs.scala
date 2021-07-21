package io.laserdisc.mysql.binlog.models

import cats.implicits._
import doobie._
import doobie.implicits._
import cats.effect.MonadCancel

case class BinaryLogs(fileName: String, size: Long)

object BinaryLogs {
  def showLogs[F[_]](implicit xa: Transactor[F], ev: MonadCancel[F, Throwable]): F[List[BinaryLogs]] =
    sql"""show binary logs""".query[BinaryLogs].to[List].transact(xa)

  def bytesBehindTheHead[F[_]: Transactor](fileName: String, pos: Long)(
    implicit ev: MonadCancel[F, Throwable]
  ): F[Long] =
    showLogs[F].map { logs =>
      logs.dropWhile(log => log.fileName != fileName).foldLeft(0L) { case (sum, l) =>
        sum + l.size
      } - pos
    }
}
