//package io.laserdisc.mysql.binlog.kinesis.examples
//
//import cats.effect.{ExitCode, IO, IOApp}
//import io.laserdisc.mysql.binlog.kinesis.{BinlogListenerContext, KinesisPublisherConfig, kinesisPublisherStream}
//import org.typelevel.log4cats.Logger
//import org.typelevel.log4cats.slf4j.Slf4jLogger
//
//object BinLogListenerExample extends IOApp {
//
//  override def run(args: List[String]): IO[ExitCode] =
//    for {
//      implicit0(log: Logger[IO]) <- Slf4jLogger.fromName[IO]("application")
//      appConfig                  <- KinesisPublisherConfig.load[IO]
//      _                          <- log.error(s"APP-STARTED:  config:$appConfig")
//      ctx                        <- BinlogListenerContext.mkContext(appConfig)
//      _                          <- log.info("STREAMING-STARTING")
//      stream                     <- kinesisPublisherStream[IO].run(ctx)
//      _                          <- stream.compile.drain
//      _                          <- log.warn("APP-SHUTDOWN")
//    } yield ExitCode.Success
//
//
//
//}
