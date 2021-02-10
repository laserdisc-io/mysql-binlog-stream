package io.laserdisc.mysql.binlog.stream

import cats.effect.{ Blocker, ConcurrentEffect, ContextShift, Sync }
import cats.implicits._
import com.github.shyiko.mysql.binlog.BinaryLogClient
import com.github.shyiko.mysql.binlog.event.Event
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.Logger

class MysSqlBinlogEventProcessor[F[_]: ConcurrentEffect: Logger](
  binlogClient: BinaryLogClient,
  queue: Queue[F, Option[Event]]
) {
  def run(): Unit = {
    binlogClient.registerEventListener { event =>
      ConcurrentEffect[F].toIO(queue.enqueue1(Some(event))).unsafeRunSync()
    }
    binlogClient.registerLifecycleListener(new BinaryLogClient.LifecycleListener {
      override def onConnect(client: BinaryLogClient): Unit =
        ConcurrentEffect[F].toIO(Logger[F].info("Connected")).unsafeRunSync

      override def onCommunicationFailure(client: BinaryLogClient, ex: Exception): Unit =
        ConcurrentEffect[F]
          .toIO(
            Logger[F].error(ex)("communication failed with") >> queue
              .enqueue1(None)
          )
          .unsafeRunSync
      override def onEventDeserializationFailure(client: BinaryLogClient, ex: Exception): Unit =
        ConcurrentEffect[F]
          .toIO(Logger[F].error(ex)("failed to deserialize event") >> queue.enqueue1(None))
          .unsafeRunSync

      override def onDisconnect(client: BinaryLogClient): Unit =
        ConcurrentEffect[F]
          .toIO(Logger[F].error("Disconnected") >> queue.enqueue1(None))
          .unsafeRunSync
    })
    binlogClient.connect()
  }
}

object MysqlBinlogStream {
  def rawEvents[F[_]: ConcurrentEffect: Logger: ContextShift](
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
