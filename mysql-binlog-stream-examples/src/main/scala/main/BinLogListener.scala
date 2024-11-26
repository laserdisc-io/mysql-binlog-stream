package main

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import ciris._
import ciris.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.types.string.TrimmedString
import io.laserdisc.mysql.binlog.config.BinLogConfig
import io.laserdisc.mysql.binlog.models.SchemaMetadata
import io.laserdisc.mysql.binlog.stream.{MysqlBinlogStream, TransactionState, streamEvents}
import io.laserdisc.mysql.binlog.{client, database}
import org.typelevel.log4cats.slf4j.Slf4jLogger

object BinLogListener extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val conf =
      (
        env("DB_HOST").as[TrimmedString],
        env("DB_PORT").as[Int],
        env("DB_USER").as[TrimmedString],
        env("DB_PASSWORD"),
        env("DB_URL").option,
        env("DB_SCHEMA"),
        env("USE_SSL").as[Boolean],
        env("SERVER_ID").as[Int]
      ).parMapN { case (host, port, user, password, url, schema, useSSL, sid) =>
        BinLogConfig(
          host,
          port,
          user,
          password,
          schema,
          poolSize = 1,
          useSSL = useSSL,
          urlOverride = url,
          serverId = Some(sid)
        )
      }.load[IO]

    Slf4jLogger.fromName[IO]("application").flatMap { implicit logger =>
      conf.flatMap { config =>
        database.transactor[IO](config).use { implicit xa =>
          for {
            // Here we do not provide binlog offset, client will be initialized with default file and offset
            binlogClient   <- client.createBinLogClient[IO](config)
            schemaMetadata <- SchemaMetadata.buildSchemaMetadata(config.schema)
            transactionState <- TransactionState
              .createTransactionState[IO](schemaMetadata, binlogClient)
            _ <- MysqlBinlogStream
              .rawEvents[IO](binlogClient)
              .through(streamEvents[IO](transactionState, config.schema))
              .evalTap(msg => logger.info(s"received $msg"))
              // Here you should do the checkpoint
              .compile
              .drain
          } yield ExitCode.Success
        }
      }
    }
  }

}
