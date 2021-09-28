package io.laserdisc.mysql.binlog.stream

import cats.effect.kernel.Async
import cats.effect.{ Concurrent, Sync }
import cats.implicits._
import com.github.shyiko.mysql.binlog.BinaryLogClient
import com.github.shyiko.mysql.binlog.event.Event
import cats.effect.std.{ Dispatcher, Queue }
import org.typelevel.log4cats.Logger

class MysSqlBinlogEventProcessor[F[_]: Concurrent: Async: Logger](
  binlogClient: BinaryLogClient,
  queue: Queue[F, Option[Event]],
  dispatcher: Dispatcher[F]
) {
  def run(): Unit = {
    binlogClient.registerEventListener { event =>
      dispatcher.unsafeRunSync(queue.offer(Some(event)))
    }
    binlogClient.registerLifecycleListener(new BinaryLogClient.LifecycleListener {
      override def onConnect(client: BinaryLogClient): Unit =
        dispatcher.unsafeRunSync(Logger[F].info("Connected"))

      override def onCommunicationFailure(client: BinaryLogClient, ex: Exception): Unit =
        dispatcher.unsafeRunSync(
          Logger[F].error(ex)("communication failed with") >> queue
            .offer(None)
        )
      override def onEventDeserializationFailure(client: BinaryLogClient, ex: Exception): Unit =
        dispatcher.unsafeRunSync(
          Logger[F].error(ex)("failed to deserialize event") >> queue.offer(None)
        )

      override def onDisconnect(client: BinaryLogClient): Unit =
        dispatcher.unsafeRunSync(Logger[F].error("Disconnected") >> queue.offer(None))
    })
    binlogClient.connect()
  }
}

object MysqlBinlogStream {
  def rawEvents[F[_]: Concurrent: Logger: Async](
    client: BinaryLogClient,
    dispatcher: Dispatcher[F]
  ): fs2.Stream[F, Event] =
    for {
      q: Queue[F, Option[Event]] <- fs2.Stream.eval(Queue.bounded[F, Option[Event]](10000))
      processor: MysSqlBinlogEventProcessor[F] <-
        fs2.Stream.eval(Sync[F].delay(new MysSqlBinlogEventProcessor[F](client, q, dispatcher)))
      event <- fs2.Stream
                 .eval(q.take)
                 .unNoneTerminate
                 .concurrently(
                   fs2.Stream
                     .eval(Sync[F].blocking(processor.run()))
                     .onFinalize(Sync[F].delay(client.disconnect()))
                 )
    } yield event
}
