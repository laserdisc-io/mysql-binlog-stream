package io.laserdisc.mysql.binlog.event

import io.circe.Json
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EventMessageTest extends AnyWordSpec with Matchers {

  "EventMessage" should {
    "be orderable by Offset" in {

      import OffsetTest.*
      import OffsetOrdering.ordering

      evtMsg(FILE_A, offset = 123) should be < evtMsg(FILE_B, offset = 999)
      evtMsg(FILE_C, offset = 123) should be > evtMsg(FILE_B, offset = 999)
      evtMsg(FILE_C, offset = 123) should be < evtMsg(FILE_C, offset = 999)
    }
  }

  def evtMsg(filename: String, offset: Long): EventMessage =
    EventMessage(
      table = "whatevertable",
      timestamp = System.currentTimeMillis(),
      action = "update",
      fileName = filename,
      offset = offset,
      endOfTransaction = true,
      pk = Json.Null,
      row = Json.Null,
      xaId = None
    )

}
