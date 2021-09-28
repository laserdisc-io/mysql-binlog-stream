package io.laserdisc.mysql.binlog

import cats.effect._
import doobie._
import doobie.hikari.HikariTransactor
import io.laserdisc.mysql.binlog.config.BinLogConfig
import cats.effect.Resource

package object database {
  def transactor[F[_]: Async](
    config: BinLogConfig
  ): Resource[F, HikariTransactor[F]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32) // our connect EC
      te <- Resource.unit[F] // our transaction EC
      _  <- Resource.eval(Sync[F].delay(Class.forName(config.driverClass)))
      xa <- HikariTransactor.newHikariTransactor[F](
              config.driverClass,
              config.connectionURL,
              config.user,
              config.password,
              ce
            )
    } yield xa

}
