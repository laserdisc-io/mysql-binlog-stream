package io.laserdisk.mysql.binlog

import cats.effect._
import doobie._
import doobie.hikari.HikariTransactor

package object database {
  def transactor(
    dbConfig: DbConfig
  )(implicit cs: ContextShift[IO]): Resource[IO, HikariTransactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32) // our connect EC
      te <- Blocker[IO] // our transaction EC
      _  <- Resource.liftF(Async[IO].delay(Class.forName(dbConfig.className)))
      xa <- HikariTransactor.newHikariTransactor[IO](
              dbConfig.className,
              dbConfig.url,
              dbConfig.user,
              dbConfig.password,
              ce,
              te
            )
    } yield xa

}
