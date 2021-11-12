package io.laserdisc.mysql.binlog.stream

import cats.effect.std.{ Dispatcher, Queue }
import cats.effect.{ Async, IO, LiftIO }
import cats.implicits._
import com.github.shyiko.mysql.binlog.BinaryLogClient
import com.github.shyiko.mysql.binlog.event.Event
import fs2.Stream
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
        dispatcher.unsafeRunAndForget(Logger[F].info("Connected"))

      override def onCommunicationFailure(client: BinaryLogClient, ex: Exception): Unit =
        dispatcher.unsafeRunAndForget(
          Logger[F].error(ex)("communication failed with") >> queue.offer(None)
        )

      override def onEventDeserializationFailure(client: BinaryLogClient, ex: Exception): Unit =
        dispatcher.unsafeRunAndForget(
          Logger[F].error(ex)("failed to deserialize event") >> queue.offer(None)
        )

      override def onDisconnect(client: BinaryLogClient): Unit =
        dispatcher.unsafeRunAndForget(Logger[F].error("Disconnected") >> queue.offer(None))
    })

    binlogClient.connect()
  }
}

object MysqlBinlogStream {

  def rawEvents[F[_]: Async: Logger: LiftIO](
    client: BinaryLogClient
  ): Stream[F, Event] =
    for {
      d   <- Stream.resource(Dispatcher[F])
      q   <- Stream.eval(Queue.bounded[F, Option[Event]](10000))
      proc = new MysSqlBinlogEventProcessor[F](client, q, d)
      /* some difficulties here during the cats3 migration.  Basically, we would have used:
       * .eval(Async[F].interruptible(many = true)(proc.run()))
       * instead of the below code to start `proc`.  Unfortunately, the binlogger library uses SocketStream.read
       * which blocks and can't be terminated normally.  See https://github.com/typelevel/fs2/issues/2362 */
      procStream = Stream
                     .eval(
                       LiftIO[F].liftIO(
                         IO.delay[Unit](proc.run()).start.flatMap(_.joinWithNever)
                       )
                     )
      evtStream <- Stream
                     .fromQueueNoneTerminated(q)
                     .concurrently(procStream)
                     .onFinalize(Async[F].delay(client.disconnect()))
    } yield evtStream

}
