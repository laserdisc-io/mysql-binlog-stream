package io.laserdisk.mysql.binlog.stream

import cats.effect.{ Blocker, ConcurrentEffect, ContextShift, Effect, IO, Sync }
import cats.implicits._
import com.github.shyiko.mysql.binlog.BinaryLogClient
import com.github.shyiko.mysql.binlog.event.Event
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger

class MysSqlBinlogEventProcessor[F[_]: Effect: SelfAwareStructuredLogger](
  binlogClient: BinaryLogClient,
  queue: Queue[F, Option[Event]]
) {
  def run(): Unit = {
    binlogClient.registerEventListener(event =>
      Effect[F]
        .runAsync(queue.enqueue1(Some(event)))(_ => IO.unit)
        .unsafeRunSync
    )
    binlogClient.registerLifecycleListener(new BinaryLogClient.LifecycleListener {
      override def onConnect(client: BinaryLogClient): Unit =
        Effect[F]
          .runAsync(SelfAwareStructuredLogger[F].info("Connected"))(_ => IO.unit)
          .unsafeRunSync
      override def onCommunicationFailure(client: BinaryLogClient, ex: Exception): Unit =
        Effect[F]
          .runAsync(
            SelfAwareStructuredLogger[F].error(ex)("communication failed with") >> queue
              .enqueue1(None)
          )(_ => IO.unit)
          .unsafeRunSync
      override def onEventDeserializationFailure(client: BinaryLogClient, ex: Exception): Unit =
        Effect[F]
          .runAsync(
            SelfAwareStructuredLogger[F].error(ex)("failed to deserialize event") >> queue
              .enqueue1(None)
          )(_ => IO.unit)
          .unsafeRunSync

      override def onDisconnect(client: BinaryLogClient): Unit =
        Effect[F]
          .runAsync(
            SelfAwareStructuredLogger[F]
              .error("Disconnected") *> queue.enqueue1(None)
          )(_ => IO.unit)
          .unsafeRunSync
    })
    binlogClient.connect()
  }
}

object MysqlBinlogStream {
  def rawEvents[F[_]: ConcurrentEffect: SelfAwareStructuredLogger: ContextShift](
    client: BinaryLogClient
  ): fs2.Stream[F, Event] =
    for {
      q         <- fs2.Stream.eval(Queue.bounded[F, Option[Event]](10000))
      processor <- fs2.Stream.eval(Sync[F].delay(new MysSqlBinlogEventProcessor[F](client, q)))
      event <- q.dequeue.unNoneTerminate concurrently fs2.Stream.eval(
                 Blocker[F].use(b => b.delay(processor.run()))
               ) onFinalize Sync[F].delay(client.disconnect())
    } yield event
}
