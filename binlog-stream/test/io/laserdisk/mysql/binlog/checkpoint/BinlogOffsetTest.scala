package io.laserdisk.mysql.binlog.checkpoint

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class BinlogOffsetTest extends AnyWordSpec with Matchers {
  "offsets" should {

    "comparable" in {
      BinlogOffset.compareOffsets(
        BinlogOffset("app1", "mysql-bin-changelog.001450", 123),
        BinlogOffset("app1", "mysql-bin-changelog.001451", 999)
      ) should be(BinlogOffset("app1", "mysql-bin-changelog.001451", 999))

      BinlogOffset.compareOffsets(
        BinlogOffset("app1", "mysql-bin-changelog.001452", 123),
        BinlogOffset("app1", "mysql-bin-changelog.001451", 999)
      ) should be(BinlogOffset("app1", "mysql-bin-changelog.001452", 123))

      BinlogOffset.compareOffsets(
        BinlogOffset("app1", "mysql-bin-changelog.001452", 123),
        BinlogOffset("app1", "mysql-bin-changelog.001452", 999)
      ) should be(BinlogOffset("app1", "mysql-bin-changelog.001452", 999))
    }

  }
}
