package io.laserdisc.mysql.binlog.kinesis.utils

import java.nio.ByteBuffer
import cats.effect.{ Concurrent, Sync, Timer }
import cats.effect.concurrent.Ref
import com.amazonaws.services.kinesis.producer.{ Attempt, UserRecordResult }
import com.google.common.util.concurrent.{ ListenableFuture, SettableFuture }
import fs2.aws.internal.KinesisProducerClient
import io.circe.Decoder
import io.circe.jawn.CirceSupportParser

import scala.jdk.CollectionConverters._
import cats.implicits._
import fs2.Stream
import fs2.concurrent.SignallingRef
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.{ DurationInt, FiniteDuration }

object TestProducer {

  /** Build a [[TestProducer]] instance with an empty state */
  def apply[F[_]: Sync, T](
    implicit decoder: Decoder[T],
    logger: Logger[F]
  ): F[TestProducer[F, T]] =
    Ref
      .of[F, List[PublishedRecord[T]]](Nil)
      .map(state => new TestProducer[F, T](state))
}

// holds the value of a call to the test KPL
case class PublishedRecord[T](
  streamName: String,
  partitionKey: String,
  data: T
)

/** A `fs2.aws.internal.KinesisProducerClient` implementation that provides some additional
  * utility functions that the fs2-aws TestKinesisProducerClient doesn't.
  */
class TestProducer[F[_], T](state: Ref[F, List[PublishedRecord[T]]])(
  implicit decoder: Decoder[T],
  logger: Logger[F]
) extends KinesisProducerClient[F] {

  // TODO: evaluate contributing this class back to fs2-aws

  /** Get the records published to this producer */
  def getPutRecords: F[List[PublishedRecord[T]]] = state.get

  // this is a copy of the putData function from the TestKinesisProducerClient
  override def putData(
    streamName: String,
    partitionKey: String,
    data: ByteBuffer
  )(implicit F: Sync[F]): F[ListenableFuture[UserRecordResult]] =
    for {
      t <- CirceSupportParser
             .parseFromByteBuffer(data)
             .toEither
             .flatMap(_.as[T])
             .liftTo[F]
      rec = PublishedRecord(streamName, partitionKey, t)
      _  <- state.modify(orig => (orig :+ rec, orig))
      res = {
        val future: SettableFuture[UserRecordResult] = SettableFuture.create()
        future.set(new UserRecordResult(List[Attempt]().asJava, "seq #", "shard #", true))
        future
      }

    } yield res

  /** Evaluate the supplied function `f`, terminating when either the function terminates, or this
    * kinesis producer client has been determined to have gone idle, whichever happens first.  Idle
    * means that no records have been dispatched for publishing over some predefined period.
    *
    * @param f The function to evaluate
    * @param idleThreshold How long to wait for new records to be published before decided that the client has gone idle
    * @param initialDelay How long to initially wait before starting idle evaluation (default is don't wait)
    */
  def haltWhenIdle[A](
    f: F[A],
    idleThreshold: FiniteDuration,
    initialDelay: FiniteDuration = 0.seconds
  )(implicit F: Concurrent[F], T: Timer[F]): F[Unit] = {

    def detectIdle(
      stateIsIdle: SignallingRef[F, Boolean]
    ): fs2.Stream[F, Unit] =
      fs2.Stream
        .eval(state.get.map(_.size))
        .evalTap(size =>
          logger.info(
            s"starting idleness detection (initialSize=$size,initialDelay=$initialDelay,idleThreshold=$idleThreshold)"
          )
        )
        .evalTap(_ => Timer[F].sleep(initialDelay))
        .evalMap(Ref.of[F, Int])
        .flatMap { prevSizeRef =>
          fs2.Stream
            .awakeEvery[F](idleThreshold)
            .evalMap { ts =>
              for {
                prevSize <- prevSizeRef.get
                curSize  <- state.get.map(_.size)
                _        <- prevSizeRef.update(_ => curSize)
                goneIdle  = prevSize == curSize
                _ <- if (goneIdle) {
                       logger.warn(
                         s"after ${ts.toMillis}ms, $curSize records produced (was $prevSize), idle=$goneIdle"
                       )
                     } else {
                       logger.info(
                         s"after ${ts.toMillis}ms, $curSize records produced (was $prevSize), idle=$goneIdle"
                       )
                     }

                _ <- stateIsIdle.update(_ => goneIdle)
              } yield ()
            }
        }

    Stream
      .eval(SignallingRef[F, Boolean](initial = false))
      .flatMap { idleSignal =>
        fs2.Stream
          .eval(f)
          .concurrently(detectIdle(idleSignal))
          .interruptWhen(idleSignal)
      }
      .compile
      .drain

  }

}
