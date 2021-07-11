package io.laserdisc.mysql.binlog.kinesis.main

import cats.effect.{ExitCode, IO, IOApp}
import io.laserdisc.mysql.binlog.kinesis.aws.Metrics
import io.laserdisc.mysql.binlog.kinesis.config.BinLogKinesisConfig
import io.laserdisc.mysql.binlog.kinesis.context.BinlogListenerContext
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object BinLogListener extends IOApp with Metrics[IO] {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      implicit0(log: Logger[IO]) <- Slf4jLogger.fromName[IO]("application")
      appConfig                  <- BinLogKinesisConfig.load[IO]
      _                          <- log.error(s"APP-STARTED: version:${BuildInfo.version} config:$appConfig")
      ctx                        <- BinlogListenerContext.mkContext(appConfig)
      _                          <- log.info("STREAMING-STARTING")
      stream                     <- mainStream[IO].run(ctx)
      _                          <- stream.compile.drain
      _                          <- log.warn("APP-SHUTDOWN")
    } yield ExitCode.Success

}
