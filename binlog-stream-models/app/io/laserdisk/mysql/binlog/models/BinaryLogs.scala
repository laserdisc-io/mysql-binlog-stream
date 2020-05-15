package io.laserdisk.mysql.binlog.models

import cats.effect.IO
import doobie._
import doobie.implicits._
case class BinaryLogs(fileName: String, size: Long)

object BinaryLogs {
  def showLogs(implicit xa: Transactor[IO]): IO[List[BinaryLogs]] =
    sql"""show binary logs""".query[BinaryLogs].to[List].transact(xa)

  def bytesBehindTheHead(fileName: String, pos: Long)(implicit xa: Transactor[IO]): IO[Long] =
    showLogs.map { logs =>
      logs.dropWhile(log => log.fileName != fileName).foldLeft(0L) {
        case (sum, l) => sum + l.size
      } - pos
    }
}
