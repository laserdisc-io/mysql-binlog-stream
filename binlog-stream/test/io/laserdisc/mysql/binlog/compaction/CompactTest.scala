package io.laserdisc.mysql.binlog.compaction

import io.circe.parser.parse
import io.laserdisc.mysql.binlog.event.EventMessage
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CompactTest extends AnyWordSpec with OptionValues with Matchers {
  "transaction compacter" should {
    def insert(id: Int) =
      EventMessage(
        "sku",
        1591212793000L,
        "create",
        "8908ecfb63e4-bin.000009",
        640,
        false,
        parse(s"""{ "id" : $id }""").toOption.value,
        parse(s"""{
           "before" : null,
           "after" : {
               "id" : $id,
               "sku" : "123"
             }
        }""").toOption.value
      )

    val update = EventMessage(
      "sku",
      1591212794000L,
      "update",
      "8908ecfb63e4-bin.000009",
      821,
      true,
      parse("""{ "id" : 1111 }""").toOption.value,
      parse("""{
           "before" : {
               "id" : 1111,
               "sku" : "123"
             },
           "after" : {
               "id" : 1111,
               "sku" : "123_up"
             }
         }""").toOption.value
    )

    val delete = EventMessage(
      "sku",
      1591214002000L,
      "delete",
      "8908ecfb63e4-bin.000009",
      1836,
      true,
      parse("""{
         "id" : 1111
       }""").toOption.value,
      parse("""{
         "before" : {
          "id" : 1111,
          "sku" : "123"
        },
         "after" : null
       }""").toOption.value
    )

    "merge insert and update" in {
      compact(Seq(insert(1111), update)) should be(
        Seq(
          EventMessage(
            "sku",
            1591212794000L,
            "create",
            "8908ecfb63e4-bin.000009",
            821,
            false,
            parse("""{ "id" : 1111 }""").toOption.value,
            parse("""{
           "before" : null,
           "after" : {
               "id" : 1111,
               "sku" : "123_up"
             }
        }""").toOption.value
          )
        )
      )
    }

    "do not produce value for insert and deletes" in {
      compact(Seq(insert(1111), delete)) should be(empty)
    }

    "do not produce value for insert update and deletes" in {
      compact(Seq(insert(1111), update, delete)) should be(empty)
    }

    "insert delete insert result in latest insert only" in {
      compact(Seq(insert(1111), delete, delete)) should be(
        Seq(
          EventMessage(
            "sku",
            1591214002000L,
            "delete",
            "8908ecfb63e4-bin.000009",
            1836,
            true,
            parse("""{
        "id" : 1111
      }""").toOption.value,
            parse("""{
        "before" : {
          "id" : 1111,
          "sku" : "123"
        },
        "after" : null
      }""").toOption.value
          )
        )
      )
    }

    "update and delete yields delete" in {
      compact(Seq(update, delete)) should be(
        Seq(
          EventMessage(
            "sku",
            1591214002000L,
            "delete",
            "8908ecfb63e4-bin.000009",
            1836,
            true,
            parse("""{
        "id" : 1111
      }""").toOption.value,
            parse("""{
        "before" : {
          "id" : 1111,
          "sku" : "123"
        },
        "after" : null
      }""").toOption.value
          )
        )
      )
    }

    "the order of the events in final stream has to be driven by last event before compaction" in {
      compact(Seq(update, insert(2222), delete)) should be(
        Seq(
          EventMessage(
            "sku",
            1591212793000L,
            "create",
            "8908ecfb63e4-bin.000009",
            640,
            false,
            parse("""{ "id" : 2222 }""").toOption.value,
            parse("""{
           "before" : null,
           "after" : {
               "id" : 2222,
               "sku" : "123"
             }
        }""").toOption.value
          ),
          EventMessage(
            "sku",
            1591214002000L,
            "delete",
            "8908ecfb63e4-bin.000009",
            1836,
            true,
            parse("""{
        "id" : 1111
      }""").toOption.value,
            parse("""{
        "before" : {
          "id" : 1111,
          "sku" : "123"
        },
        "after" : null
      }""").toOption.value
          )
        )
      )
    }
  }
}
