package io.laserdisc.mysql.binlog.event

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import OffsetTest._

object OffsetTest {
  val FILE_A = "mysql-bin-changelog.001450"
  val FILE_B = "mysql-bin-changelog.001451"
  val FILE_C = "mysql-bin-changelog.001452"
}

class OffsetTest extends AnyWordSpec with Matchers {

  case class Foo(
    override val fileName: String,
    override val offset: Long
  ) extends Offset

  "OrderedBinLogOffset" should {

    "have natural ordering" in {

      import OffsetOrdering.ordering

      Foo(FILE_A, 123) should be < Foo(FILE_B, 999)
      Foo(FILE_C, 123) should be > Foo(FILE_B, 999)
      Foo(FILE_C, 123) should be < Foo(FILE_C, 999)

    }

    "have same ordering with explicit ordering" in {

      OffsetOrdering.ordering.compare(Foo(FILE_A, 123), Foo(FILE_B, 999)) should be < 0
      OffsetOrdering.ordering.compare(Foo(FILE_C, 123), Foo(FILE_B, 999)) should be > 0
      OffsetOrdering.ordering.compare(Foo(FILE_C, 123), Foo(FILE_C, 999)) should be < 0
      OffsetOrdering.ordering.compare(Foo(FILE_C, 999), Foo(FILE_C, 999)) should equal(0)

    }
  }

}
