package io.laserdisc.mysql.binlog.stream

import cats.effect.std.{ Dispatcher, Queue }
import cats.effect.{ Async, Sync }
import cats.implicits._
import com.github.shyiko.mysql.binlog.BinaryLogClient
import com.github.shyiko.mysql.binlog.event.Event
import fs2.Stream
import fs2.concurrent.SignallingRef
import org.typelevel.log4cats.Logger

class MysSqlBinlogEventProcessor[F[_]: Async: Logger](
  binlogClient: BinaryLogClient,
  queue: Queue[F, Option[Event]],
  dispatcher: Dispatcher[F]
) {
  def run(): Unit = {
    binlogClient.registerEventListener { event =>
      dispatcher.unsafeRunAndForget(queue.offer(Some(event)))
    }
    binlogClient.registerLifecycleListener(new BinaryLogClient.LifecycleListener {
      override def onConnect(client: BinaryLogClient): Unit =
        dispatcher.unsafeRunAndForget(Logger[F].info("Connected"))

      override def onCommunicationFailure(client: BinaryLogClient, ex: Exception): Unit =
        dispatcher.unsafeRunAndForget(
          Logger[F].error(ex)("communication failed with") >> queue.offer(None)
        )

      override def onEventDeserializationFailure(client: BinaryLogClient, ex: Exception): Unit =
        dispatcher.unsafeRunAndForget(
          Logger[F].error(ex)("failed to deserialize event") >> queue.offer(None)
        )

      override def onDisconnect(client: BinaryLogClient): Unit = {
        println("LifecycleListener.onDisconnect - start")
        dispatcher.unsafeRunAndForget(Logger[F].error("Disconnected") >> queue.offer(None))
        println("LifecycleListener.onDisconnect - end")
      }
    })
    println("binlogClient.connect() - start")
    binlogClient.connect()
    println("binlogClient.connect() - end")
  }
}

object MysqlBinlogStream {

  def rawEvents[F[_]: Async: Logger](
    client: BinaryLogClient
  ): Stream[F, Event] =
    for {
      dispatcher <- Stream.resource(Dispatcher[F])
      q          <- Stream.eval(Queue.bounded[F, Option[Event]](10000))
      processor <-
        Stream.eval(Sync[F].delay(new MysSqlBinlogEventProcessor[F](client, q, dispatcher)))
      event <- Stream
                 .fromQueueNoneTerminated(q)
                 .onFinalize(Logger[F].error("shutting down main stream"))
                 .concurrently {
                   val eff: F[Unit] = Sync[F].interruptible(many = false)(processor.run())
                   Stream
                     .eval(Logger[F].warn("starting processor"))
                     .onFinalize(Logger[F].error("REQ SHUTDOWN"))
                     .evalMap(_ => eff)
                     .evalTap(_ => Logger[F].warn("elem"))
                     .onFinalize(
                       Logger[F].error("shutting down") >>
                         Sync[F].delay(client.disconnect())
                     )
                 }
    } yield event

  /*
   def rawEvents[F[_]: ConcurrentEffect: Logger: ContextShift](client: BinaryLogClient): fs2.Stream[F, Event] =
    for {
      q         <- fs2.Stream.eval(Queue.bounded[F, Option[Event]](10000))
      processor <- fs2.Stream.eval(Sync[F].delay(new MysSqlBinlogEventProcessor[F](client, q)))
      event <- q.dequeue.unNoneTerminate concurrently fs2.Stream.eval(
                 Blocker[F].use(b => b.delay(processor.run()))
               ) onFinalize Sync[F].delay(client.disconnect())
    } yield event
   */

  def szilard[F[_]: Async: Logger](
    client: BinaryLogClient
  ): Stream[F, Event] =
    for {
      q          <- Stream.eval(Queue.bounded[F, Option[Event]](10000))
      dispatcher <- Stream.resource(Dispatcher[F])
      processor <-
        Stream.eval(Async[F].delay(new MysSqlBinlogEventProcessor[F](client, q, dispatcher)))
      event <- Stream
                 .eval(q.take)
                 .unNoneTerminate
                 .concurrently(
                   // this is not stopping when the calling stream terminates
                   Stream
                     .eval(Async[F].blocking(processor.run()))
                     .onFinalize(Async[F].delay(client.disconnect()))
                 )
    } yield event
}
