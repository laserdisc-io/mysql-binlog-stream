package io.laserdisc.mysql.binlog.stream

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.ForAllTestContainer
import com.github.shyiko.mysql.binlog.BinaryLogClient
import db.MySqlContainerTest
import doobie.implicits.*
import io.circe.optics.JsonPath.root
import io.laserdisc.mysql.binlog.database
import io.laserdisc.mysql.binlog.config.BinLogConfigOps
import io.laserdisc.mysql.binlog.event.EventMessage
import io.laserdisc.mysql.binlog.models.SchemaMetadata
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.DurationInt

/** Coverage for the issue fixed in https://github.com/laserdisc-io/mysql-binlog-stream/pull/355
  *
  * Basically, in the scenario where the same table exists in multiple schemas, the SchemaMetadata can get confused and attempt to use the
  * wrong schema definition for the table in another schemas.
  */
class MultiSchemaTest extends AnyWordSpec with ForAllTestContainer with MySqlContainerTest with Matchers {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def runScenarioForSchema(schema: String): List[EventMessage] = {

    val client     = binlogConfig.mkBinaryLogClient()
    val xaResource = database.transactor[IO](binlogConfig)

    client.registerLifecycleListener(new BinaryLogClient.AbstractLifecycleListener {
      override def onConnect(client: BinaryLogClient): Unit =
        xaResource
          .use(xa =>
            (
              // there are two schemas, both with `test_table`, same columns, but differing types
              // see init.sql for definitions
              for {
                _ <- sql"""truncate table multi_schema_test_a.test_table""".update.run
                _ <- sql"""truncate table multi_schema_test_b.test_table""".update.run
                // alternate inserts into the two tables so we can confirm ordering
                _ <- sql"""insert into multi_schema_test_a.test_table (id, payload) values (1, "leela")""".update.run
                _ <- sql"""insert into multi_schema_test_b.test_table (id, payload) values ("one", now())""".update.run
                _ <- sql"""insert into multi_schema_test_a.test_table (id, payload) values (2, "zoidberg")""".update.run
                _ <- sql"""insert into multi_schema_test_b.test_table (id, payload) values ("two", now())""".update.run
              } yield ()
            ).transact(xa)
          )
          .unsafeRunSync()
    })

    val r = for {
      meta     <- xaResource.use(implicit xa => SchemaMetadata.buildSchemaMetadata(schema))
      txnState <- TransactionState.createTransactionState[IO](meta, client)
      res <-
        MysqlBinlogStream
          .rawEvents[IO](client)
          .through(streamCompactedEvents[IO](txnState, schema))
          .filter(_.action == "create")
          .take(2)
          .interruptAfter(5.seconds)
          .compile
          .toList
    } yield res

    r.unsafeRunSync()
  }

  "Binlog stream" should {

    "handle multi-schema events" in {

      val idPath      = root.after.id
      val payloadPath = root.after.payload

      val eventsA = runScenarioForSchema("multi_schema_test_a")
      eventsA should have size 2
      idPath.int.getOption(eventsA(0).row) should be(Some(1))
      idPath.int.getOption(eventsA(1).row) should be(Some(2))
      payloadPath.string.getOption(eventsA(0).row) should be(Some("leela"))
      payloadPath.string.getOption(eventsA(1).row) should be(Some("zoidberg"))

      val eventsB = runScenarioForSchema("multi_schema_test_b")
      eventsB should have size 2
      idPath.string.getOption(eventsB(0).row) should be(Some("one"))
      idPath.string.getOption(eventsB(1).row) should be(Some("two"))
      payloadPath.long.getOption(eventsB(0).row).get should be > 169765900000000L
      payloadPath.long.getOption(eventsB(1).row).get should be > 169765900000000L

    }
  }
}
