package io.laserdisc.mysql.binlog

import cats.effect.{ ConcurrentEffect, ContextShift, Timer }
import fs2.Stream
import fs2.aws.kinesis.publisher
import io.circe.Printer
import io.circe.generic.auto._
import io.circe.syntax._
import io.laserdisc.mysql.binlog.checkpoint.BinlogOffset
import io.laserdisc.mysql.binlog.event.EventMessage
import io.laserdisc.mysql.binlog.stream._
import org.typelevel.log4cats.Logger

import java.nio.ByteBuffer

package object kinesis {

  implicit val jsonEncoder: EventMessage => ByteBuffer = { msg =>
    Printer.noSpaces.printToByteBuffer(msg.asJson)
  }

  def calculateKey(payload: EventMessage): String =
    s"${payload.table}|${json.flatSort(payload.pk).mkString("|")}"

  def kinesisPublisherStream[F[_]: ConcurrentEffect: ContextShift: Timer](
    config: PublisherConfig[F]
  )(
    implicit logger: Logger[F]
  ): Stream[F, BinlogOffset] =
    fs2.Stream
      .eval(PublisherContext(config))
      .flatMap { context =>
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
          .through(Checkpointing.saveOffsetCheckpoint[F](context.dynamoDBClient, context.config))
      }

}
