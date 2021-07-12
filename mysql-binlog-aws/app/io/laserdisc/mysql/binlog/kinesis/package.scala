package io.laserdisc.mysql.binlog

import cats.data.Kleisli
import cats.effect.concurrent.Ref
import cats.effect.{ Async, Concurrent, ConcurrentEffect, ContextShift, IO, Sync, Timer }
import cats.implicits._
import com.amazonaws.services.kinesis.producer.UserRecordResult
import com.github.shyiko.mysql.binlog.BinaryLogClient
import com.google.common.util.concurrent.{ FutureCallback, Futures, ListenableFuture }
import fs2.{ Pipe, Stream }
import fs2.aws.internal.{ KinesisProducerClient, KinesisProducerClientImpl }
import fs2.aws.kinesis.publisher
import org.typelevel.log4cats.Logger
import io.circe.Printer
import io.circe.generic.auto._
import io.circe.syntax._
import io.laserdisc.mysql.binlog.checkpoint.BinlogOffset
import io.laserdisc.mysql.binlog.config.BinLogConfig
import io.laserdisc.mysql.binlog.database
import io.laserdisc.mysql.binlog.event.EventMessage
import io.laserdisc.mysql.binlog.models.SchemaMetadata
import io.laserdisc.mysql.binlog.stream._

import java.nio.ByteBuffer
import java.util.concurrent.Executors
import scala.concurrent.{ ExecutionContext, ExecutionContextExecutorService }
import scala.jdk.CollectionConverters._

package object kinesis {

  implicit val jsonEncoder: EventMessage => ByteBuffer = { msg =>
    Printer.noSpaces.printToByteBuffer(msg.asJson)
  }

  def calculateKey(payload: EventMessage): String =
    s"${payload.table}|${json.flatSort(payload.pk).mkString("|")}"

  implicit val ec: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  def kinesisPublisherStream[F[_]: ConcurrentEffect: ContextShift: Timer](
    implicit logger: Logger[F]
  ): Kleisli[F, BinlogListenerContext[F], Stream[F, BinlogOffset]] =
    Kleisli[F, BinlogListenerContext[F], Stream[F, BinlogOffset]] { context =>
      Sync[F].delay(
        MysqlBinlogStream
          .rawEvents[F](context.binaryLogClient)
          .through(streamCompactedEvents[F](context.transactionState))
          .map(em => calculateKey(em) -> em)
          .through(_.evalTap { case (key, msg) =>
            logger.info(s"KINESIS-PUBLISH key=$key msg=${msg.asJson.noSpaces}")
          })
          .through(
            publisher.writeAndForgetObjectToKinesis[F, EventMessage](
              context.config.kinesisOutputStream,
              10,
              context.kinesisProducer
            )
          )
          .handleErrorWith {
            case e if e.getMessage.contains("Data must be less than or equal to 1MB in size") =>
              Stream.eval(logger.error(e)("Skipping large message"))
            case e =>
              Stream.eval(logger.error(e)(s"Binlogger crashing on: $e")) ++
                Stream.raiseError[F](e)
          }
          .collect { case EventMessage(_, _, _, _, fileName, offset, true, _, _) =>
            BinlogOffset(context.config.checkpointAppName, fileName, offset)
          }
          .debounce(context.config.checkpointEvery)
          //          .through(scanKPLErrors(appConfig = context.appConfig))
          .through(Checkpointing.checkpoint[F](context.dynamoDbAsyncClient, context.config))
      )
    }

  def urToAttemptsMsgs(ur: UserRecordResult): String =
    ur.getAttempts.asScala.map(a => a.getErrorMessage).mkString(" | ")

  def makeTransactionState(
    config: BinLogConfig,
    binlogClient: BinaryLogClient
  )(
    implicit cs: ContextShift[IO],
    logger: Logger[IO]
  ): IO[Ref[IO, TransactionState]] =
    database
      .transactor[IO](config)
      .use(implicit xa => SchemaMetadata.buildSchemaMetadata(config.schema))
      .flatMap(sm => TransactionState.createTransactionState[IO](sm, binlogClient))

  def createKinesisProducer[F[_]: Sync](
    config: KinesisPublisherConfig
  )(implicit logger: Logger[F]): F[KinesisProducerClient[F]] =
    for {
      kpc <- Sync[F].delay(new KinesisProducerClientImpl[F] {
               override val region: Option[String] = Some(config.kinesisRegion.id)
             })
      _ <- logger.info(s"KINESIS-PRODUCER-INIT region:${config.kinesisRegion.id}")
    } yield kpc

  def scanKPLErrors[F[_]: Concurrent: Timer](
    appConfig: KinesisPublisherConfig
  ): Pipe[F, (EventMessage, ListenableFuture[UserRecordResult]), BinlogOffset] =
    _.groupWithin(Int.MaxValue, appConfig.checkpointEvery)
      .evalMap { chunk =>
        chunk
          .map { case (_, lf) =>
            Async[F]
              .async[UserRecordResult] { cb =>
                Futures.addCallback(
                  lf,
                  new FutureCallback[UserRecordResult] {
                    override def onFailure(t: Throwable): Unit             = cb(Left(t))
                    override def onSuccess(result: UserRecordResult): Unit = cb(Right(result))
                  },
                  (command: Runnable) => ec.execute(command)
                )
              }
          }
          .sequence
          .map(v => v.zip(chunk.map(_._1)))
      }
      .flatMap(Stream.chunk)
      .evalMap { case (ur, em) =>
        if (!ur.isSuccessful) {
          Sync[F].raiseError[EventMessage](
            new RuntimeException(
              s"failed to produce record $em, with following ${urToAttemptsMsgs(ur)}"
            )
          )
        } else {
          Sync[F].pure(em)
        }
      }
      .collect { case EventMessage(_, _, _, _, fileName, offset, true, _, _) =>
        BinlogOffset(appConfig.checkpointAppName, fileName, offset)
      }
}
