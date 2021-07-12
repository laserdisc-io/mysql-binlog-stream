package io.laserdisc.mysql.binlog.kinesis

import io.circe.generic.auto._
import io.circe.jawn.CirceSupportParser
import io.laserdisc.mysql.binlog.event.EventMessage
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{ EitherValues, OptionValues }

class KinesisDestinationTest extends AnyWordSpec with Matchers with OptionValues with EitherValues {

  "Kinesis destination" should {
    "calculate partition key" in {
      val raw = """{
        |  "table" : "triggered_queue_items",
        |  "timestamp" : 1552594855000,
        |  "fileName" : "acd03b3a873f-bin.000003",
        |  "offset" : 2177,
        |  "endOfTransaction" : false,
        |  "action" : "create",
        |  "pk" : {
        |    "id" : 8177515,
        |    "action" : 1
        |  },
        |  "row" : {
        |    "id" : 8177515,
        |    "entity_id" : 417244,
        |    "entity_type" : 23,
        |    "action" : 1,
        |    "status" : 0,
        |    "process_started_at" : null,
        |    "lock_version" : 0
        |  }
        |}""".stripMargin

      val actual = CirceSupportParser
        .parseFromString(raw)
        .toEither
        .flatMap(_.as[EventMessage]) match {
        case Left(v)  => fail(s"Failed to parse $raw as an EventMessage", v)
        case Right(e) => calculateKey(e)
      }

      actual should be("triggered_queue_items|action:1|id:8177515")

    }
  }

}
