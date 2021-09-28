package io.laserdisc.mysql.binlog.stream

import cats.effect.Async
import cats.implicits._
import com.github.shyiko.mysql.binlog.BinaryLogClient
import com.github.shyiko.mysql.binlog.event.Event
import cats.effect.std.{ Dispatcher, Queue }
import org.typelevel.log4cats.Logger

class MysSqlBinlogEventProcessor[F[_]: Async: Logger](
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
          Logger[F].error(ex)("communication failed with") >> queue.offer(None)
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
  def rawEvents[F[_]: Async: Logger](
    client: BinaryLogClient
  ): fs2.Stream[F, Event] =
    for {
      q          <- fs2.Stream.eval(Queue.bounded[F, Option[Event]](10000))
      dispatcher <- fs2.Stream.resource(Dispatcher[F])
      processor <-
        fs2.Stream.eval(Async[F].delay(new MysSqlBinlogEventProcessor[F](client, q, dispatcher)))
      event <- fs2.Stream
                 .eval(q.take)
                 .unNoneTerminate
                 .concurrently(
                   fs2.Stream
                     .eval(Async[F].blocking(processor.run()))
                     .onFinalize(Async[F].delay(client.disconnect()))
                 )
    } yield event
}
