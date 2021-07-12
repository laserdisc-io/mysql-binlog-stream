package io.laserdisc.mysql.binlog.kinesis

import io.circe.generic.auto._
import cats.effect.{ IO, Timer }
import cats.implicits._
import doobie.hikari.HikariTransactor
import fs2.concurrent.SignallingRef
import io.laserdisc.mysql.binlog.event.EventMessage
import io.laserdisc.mysql.binlog.kinesis.utils.TestProducer
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class MainStreamTest extends BinLogDockerSpec with Matchers {

  implicit val t: Timer[IO] = IO.timer(ExecutionContext.global)

  "binlog listener" should {
    "receive DB events" in {
      val res = runTest(3, 0).unsafeRunSync()
      res should have size 15
    }

    "resume from checkpoint" in {
      val res = runTest(2, 15).unsafeRunSync()
      res should have size 10
    }
  }

  def runTest(transactions: Int, offset: Int): IO[List[EventMessage]] =
    for {
      implicit0(logger: Logger[IO]) <- Slf4jLogger.fromName[IO]("application")
      producer                      <- TestProducer[IO, EventMessage]
      context                        = mkTestConfig[IO](producer)
      transactor                    <- mkTransactor
      signal                        <- SignallingRef[IO, Boolean](false)
      stream                         = kinesisPublisherStream[IO](context)
      _ <-
        transactor.use { xa =>
          stream
            .interruptWhen(signal)
            .concurrently(generateLoad(transactions, offset, xa, signal))
            .compile
            .drain
        }
      producedMsgs <- producer.getPutRecords
    } yield producedMsgs.map(_.data)

  def generateLoad(
    transactions: Int,
    offset: Int,
    xa: HikariTransactor[IO],
    sr: SignallingRef[IO, Boolean]
  ): fs2.Stream[IO, Unit] =
    fs2.Stream
      .awakeEvery[IO](1 second)
      .zipWithIndex
      .take(transactions)
      .evalMap { case (_, idx) => inserts(idx.toInt, offset, 5, xa) }
      .onFinalize(IO.sleep(5 seconds) >> sr.update(_ => true))

}
