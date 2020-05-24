package io.laserdisc.mysql.binlog

import cats.effect._
import doobie._
import doobie.hikari.HikariTransactor

package object database {
  def transactor[F[_]: Async: ContextShift](dbConfig: DbConfig): Resource[F, HikariTransactor[F]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32) // our connect EC
      te <- Blocker[F] // our transaction EC
      _  <- Resource.liftF(Sync[F].delay(Class.forName(dbConfig.className)))
      xa <- HikariTransactor.newHikariTransactor[F](
              dbConfig.className,
              dbConfig.url,
              dbConfig.user,
              dbConfig.password,
              ce,
              te
            )
    } yield xa

}
