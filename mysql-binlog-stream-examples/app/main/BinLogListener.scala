package main

import cats.effect.{ ExitCode, IO, IOApp }
import cats.implicits._
import ciris._
import ciris.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.types.string.TrimmedString
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.laserdisk.mysql.binlog.config.BinLogConfig
import io.laserdisk.mysql.binlog.database.DbConfig
import io.laserdisk.mysql.binlog.models.SchemaMetadata
import io.laserdisk.mysql.binlog.stream.{ streamEvents, MysqlBinlogStream, TransactionState }
import io.laserdisk.mysql.binlog.{ client, database }

object BinLogListener extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val confs =
      (
        env("DB_HOST").as[TrimmedString],
        env("DB_PORT").as[Int],
        env("DB_USER").as[TrimmedString],
        env("DB_PASSWORD"),
        env("DB_URL"),
        env("DB_SCHEMA"),
        env("USE_SSL").as[Boolean]
      ).mapN {
          case (host, port, user, password, url, schema, useSSL) =>
            (
              BinLogConfig(host, user, password, schema, port = port, useSSL = useSSL),
              DbConfig(user, password, url, 1)
            )
        }
        .load[IO]

    confs.flatMap {
      case (binLogConfig, dbConfig) =>
        implicit val bc = binLogConfig
        database.transactor[IO](dbConfig).use { implicit xa =>
          for {
            implicit0(logger: SelfAwareStructuredLogger[IO]) <- Slf4jLogger
                                                                  .fromName[IO]("application")
            //Here we do not provide binlog offset, client will be initialized with default file and offset
            binlogClient   <- client.createBinLogClient[IO](IO.pure(None))
            schemaMetadata <- SchemaMetadata.buildSchemaMetadata(binLogConfig.schema)
            transactionState <- TransactionState
                                  .createTransactionState[IO](schemaMetadata, binlogClient)
            _ <- MysqlBinlogStream
                   .rawEvents[IO](binlogClient)
                   .through(streamEvents[IO](transactionState))
                   .evalTap(msg => logger.info(s"received $msg"))
                   //Here you should do the checkpoint
                   .compile
                   .drain
          } yield (ExitCode.Success)
        }
    }
  }

}
