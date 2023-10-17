package io.laserdisc.mysql.binlog.stream

import _root_.io.circe.optics.JsonPath._
import _root_.io.laserdisc.mysql.binlog._
import com.github.shyiko.mysql.binlog.event._
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.{io, util}
import scala.collection.immutable.Queue
import scala.collection.mutable

class TransactionStateTest extends AnyWordSpec with Matchers with OptionValues {

  def createRotateEvent: Event = {
    val header = new EventHeaderV4()
    header.setEventType(EventType.ROTATE)
    header.setTimestamp(System.currentTimeMillis())
    val data = new RotateEventData()
    data.setBinlogFilename("file.123")
    data.setBinlogPosition(4)
    new Event(header, data)
  }
  def createBeginEvent: Event = {
    val header = new EventHeaderV4()
    header.setEventType(EventType.QUERY)
    header.setTimestamp(System.currentTimeMillis())
    val data = new QueryEventData()
    data.setSql("BEGIN")
    new Event(header, data)
  }
  def createInsertEvent(): Event = {
    val header = new EventHeaderV4()
    header.setEventType(EventType.EXT_WRITE_ROWS)
    header.setTimestamp(System.currentTimeMillis())
    val data = new WriteRowsEventData()
    val rows: util.List[Array[io.Serializable]] =
      new util.ArrayList[Array[io.Serializable]]()
    rows.add(
      Array(543.asInstanceOf[io.Serializable], "sku3".getBytes.asInstanceOf[io.Serializable])
    )
    data.setRows(rows)
    val set = new util.BitSet()
    set.set(0, 2)
    data.setIncludedColumns(set)
    data.setTableId(123L)
    new Event(header, data)
  }

  def createTableMapEvent(): Event = {
    val header = new EventHeaderV4()
    header.setEventType(EventType.TABLE_MAP)
    header.setTimestamp(System.currentTimeMillis())
    val data = new TableMapEventData()
    data.setTableId(123)
    data.setTable("sku")
    data.setColumnTypes(Array(3.asInstanceOf[Byte], 15.asInstanceOf[Byte]))
    new Event(header, data)
  }

  def createXAEvent(): Event = {
    val header = new EventHeaderV4()
    header.setEventType(EventType.XID)
    header.setTimestamp(System.currentTimeMillis())
    val data = new XidEventData()
    data.setXid(122345)
    new Event(header, data)
  }

  def createPreviousGTIDsEvent(): Event = {
    val header = new EventHeaderV4()
    header.setEventType(EventType.PREVIOUS_GTIDS)
    header.setTimestamp(System.currentTimeMillis())
    val data = new XidEventData()
    data.setXid(122345)
    new Event(header, data)
  }

  "Transaction State" should {
    "start transaction on BEGIN event" in {
      TransactionState
        .nextState(createBeginEvent)
        .run(
          TransactionState(
            Queue.empty,
            schemaMetadata = models.SchemaMetadata.empty,
            fileName = "file.123",
            offset = 0,
            timestamp = 0
          )
        )
        .value match {
        case (state, None) =>
          state.transactionEvents should have size 0
          state.isTransaction should be(true)
        case res => fail(s"Assertion failed with $res")
      }
    }
    "accumulate events within transaction" in {
      val skuMeta =
        models.TableMetadata(
          "sku",
          Map(
            1 -> models.ColumnMetadata("id", "int", 1, isPk = true),
            2 -> models.ColumnMetadata("sku", "varchar", 2, isPk = false)
          )
        )
      val schemaMeta =
        models
          .SchemaMetadata(tables = Map("sku" -> skuMeta), idToTable = mutable.Map(123L -> skuMeta))
      val res = for {
        _      <- TransactionState.nextState(createRotateEvent)
        _      <- TransactionState.nextState(createBeginEvent)
        _      <- TransactionState.nextState(createTableMapEvent())
        _      <- TransactionState.nextState(createInsertEvent())
        _      <- TransactionState.nextState(createTableMapEvent())
        _      <- TransactionState.nextState(createInsertEvent())
        _      <- TransactionState.nextState(createPreviousGTIDsEvent()) // should be ignored
        events <- TransactionState.nextState(createXAEvent())
      } yield events
      res
        .run(
          TransactionState(
            Queue.empty,
            schemaMetadata = schemaMeta,
            fileName = "file.1234",
            offset = 0,
            timestamp = 0
          )
        )
        .value match {
        case (state, txnPackage) =>
          state.isTransaction should be(false)
          state.transactionEvents should be(empty)
          txnPackage.value.events should have size 2
          txnPackage.value.events.forall(_.fileName == "file.123") should be(true)
          txnPackage.value.events.map(_.endOfTransaction) should be(List(false, true))
      }
    }

    "transform binlog write event into json" in {

      val TableName = "all_types_table"

      val allTypes =
        List("int", "tinyint", "bigint", "date", "datetime", "decimal", "float", "text", "tinytext", "mediumtext", "longtext", "varchar")

      val columnMeta = allTypes.zipWithIndex.map { case (mType, idx) =>
        val colName = s"${mType}Col"
        val ordinal = idx + 1
        ordinal -> models.ColumnMetadata(colName, mType, ordinal, isPk = colName == "intCol")
      }.toMap

      val tableMeta = models.TableMetadata(TableName, columnMeta)

      val schemaMeta = models.SchemaMetadata(
        tables = Map("all_types_table" -> tableMeta),
        idToTable = mutable.Map(123L -> tableMeta)
      )

      val json = TransactionState.convertToJson(
        tableMeta = schemaMeta.tables(TableName),
        includedColumns = 0.until(columnMeta.size).toArray,
        timestamp = 12345L,
        action = "create",
        fileName = "file.12345",
        offset = 5363,
        record = (
          None,
          Some(
            Array(
              Some(100.asInstanceOf[io.Serializable]),                                    // int
              Some(200.asInstanceOf[io.Serializable]),                                    // tinyint
              Some(Long.MaxValue.asInstanceOf[io.Serializable]),                          // bigint
              Some(1672531200000L.asInstanceOf[io.Serializable]),                         // date
              Some(1672567872000L.asInstanceOf[io.Serializable]),                         // datetime
              Some(java.math.BigDecimal.valueOf(99887766).asInstanceOf[io.Serializable]), // decimal
              Some(111.222f.asInstanceOf[io.Serializable]),                               // float
              Some("some text".getBytes.asInstanceOf[io.Serializable]),                   // text
              Some("some tinytext".getBytes.asInstanceOf[io.Serializable]),               // tinytext
              Some("some mediumtext".getBytes.asInstanceOf[io.Serializable]),             // mediumtext
              Some("some longtext".getBytes.asInstanceOf[io.Serializable]),               // longtext
              Some("a varchar".getBytes.asInstanceOf[io.Serializable])                    // varchar
            )
          )
        )
      )

      json.table should be(TableName)
      json.timestamp should be(12345L)

      val after = root.after

      after.intCol.int.getOption(json.row).value should be(100)
      after.tinyintCol.int.getOption(json.row).value should be(200)
      after.bigintCol.long.getOption(json.row).value should be(Long.MaxValue)
      after.dateCol.long.getOption(json.row).value should be(1672531200000L)
      after.datetimeCol.long.getOption(json.row).value should be(1672567872000L)
      after.decimalCol.bigDecimal.getOption(json.row).value should be(BigDecimal.valueOf(99887766))
      after.floatCol.double.getOption(json.row).value should be(111.222)
      after.textCol.string.getOption(json.row).value should be("some text")
      after.tinytextCol.string.getOption(json.row).value should be("some tinytext")
      after.mediumtextCol.string.getOption(json.row).value should be("some mediumtext")
      after.longtextCol.string.getOption(json.row).value should be("some longtext")
      after.varcharCol.string.getOption(json.row).value should be("a varchar")

    }

    "extract 'truncated table sku' from SQL" in {
      models.QueryEventData.truncateTable("truncate table sku").value should be("sku")
    }

    "extract 'truncated sku' from SQL" in {
      models.QueryEventData.truncateTable("truncate sku").value should be("sku")
    }
  }
}
