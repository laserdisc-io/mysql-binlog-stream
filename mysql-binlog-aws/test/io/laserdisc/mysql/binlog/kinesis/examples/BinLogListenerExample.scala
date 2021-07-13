package io.laserdisc.mysql.binlog.kinesis.examples

import cats.effect.{ ExitCode, IO, IOApp }
import io.laserdisc.mysql.binlog.kinesis.kinesisPublisherStream
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object BinLogListenerExample extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      implicit0(log: Logger[IO]) <- Slf4jLogger.fromName[IO]("application")
      config                     <- IO.delay(???)
      _                          <- kinesisPublisherStream[IO](config).compile.drain

    } yield ExitCode.Success

}
