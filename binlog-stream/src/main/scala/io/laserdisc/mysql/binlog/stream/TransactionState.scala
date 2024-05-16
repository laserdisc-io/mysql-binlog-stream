package io.laserdisc.mysql.binlog.stream

import cats.data.State
import cats.effect.{Ref, Sync}
import cats.implicits.*
import com.github.shyiko.mysql.binlog.BinaryLogClient
import com.github.shyiko.mysql.binlog.event.EventType.{EXT_UPDATE_ROWS, UPDATE_ROWS}
import com.github.shyiko.mysql.binlog.event.deserialization.json.JsonBinary
import com.github.shyiko.mysql.binlog.event.{Event, EventData, EventHeaderV4 as JEventHeaderV4, EventType}
import io.circe.Json
import io.laserdisc.mysql.binlog.event.EventMessage
import io.laserdisc.mysql.binlog.models.*
import org.typelevel.log4cats.Logger

import java.math.BigDecimal
import scala.collection.immutable.Queue
import scala.reflect.ClassTag

case class TransactionState(
    transactionEvents: Queue[EventMessage],
    start: Long = 0,
    end: Long = 0,
    timestamp: Long,
    fileName: String,
    offset: Long,
    schemaMetadata: SchemaMetadata
) {
  def assemblePackage: TransactionPackage =
    TransactionPackage(
      events = transactionEvents.toList,
      offset = offset,
      transactionDuration = time
    )

  def isTransaction: Boolean = start != 0 && end == 0

  def time: Long = end - start
}

case class TransactionPackage(
    events: List[EventMessage],
    offset: Long,
    transactionDuration: Long
)

object TransactionState {
  type Row = Array[Option[Serializable]]

  def nextState(event: Event, schema: String = null): State[TransactionState, Option[TransactionPackage]] =
    State[TransactionState, Option[TransactionPackage]] { implicit transactionState =>
      (event.getHeader[JEventHeaderV4], event.getData[EventData]) match {
        case (EventHeaderV4(EventType.FORMAT_DESCRIPTION, _, offset), _) =>
          (transactionState.copy(offset = offset), None)
        case (EventHeaderV4(EventType.ROTATE, _, _), RotateEventData(fileName, offset)) =>
          (transactionState.copy(fileName = fileName, offset = offset), None)

        case (
              EventHeaderV4(EventType.QUERY, timestamp, offset),
              QueryEventData("begin", _, _, _)
            ) =>
          (transactionState.copy(start = timestamp, offset = offset), None)

        case (EventHeaderV4(EventType.TABLE_MAP, _, offset), TableMapEventData(tableId, database, name)) =>
          if (schema == null || database == schema) {
            transactionState.schemaMetadata.tables
              .get(name)
              .foreach(transactionState.schemaMetadata.idToTable(tableId) = _)
          }
          (transactionState.copy(offset = offset), None)

        case (
              EventHeaderV4(EventType.EXT_WRITE_ROWS | EventType.WRITE_ROWS, timestamp, offset),
              WriteRowsEventData(tableId, rows, includedColumns)
            ) =>
          handleCreate(tableId, offset, timestamp, rows, includedColumns)

        case (
              EventHeaderV4(EXT_UPDATE_ROWS | UPDATE_ROWS, timestamp, offset),
              UpdateRowsEventData(tableId, beforeAfter, includedColumns)
            ) =>
          handleUpdate(tableId, offset, timestamp, beforeAfter, includedColumns)

        case (
              EventHeaderV4(EventType.EXT_DELETE_ROWS | EventType.DELETE_FILE, timestamp, offset),
              DeleteRowsEventData(tableId, rows, columns)
            ) =>
          handleDelete(tableId, offset, timestamp, rows, columns)

        case (EventHeaderV4(EventType.XID, timestamp, offset), XidEventData(xaId)) =>
          handleCommit(transactionState, offset, timestamp, Some(xaId))
        case (
              EventHeaderV4(EventType.QUERY, timestamp, offset),
              QueryEventData("commit", _, _, _)
            ) =>
          handleCommit(transactionState, offset, timestamp, None)

        case (
              EventHeaderV4(EventType.QUERY, timestamp, offset),
              QueryEventData(_, _, sqlAction, Some(table))
            ) =>
          handleDdl(table, timestamp, offset, sqlAction)

        case (EventHeaderV4(EventType.QUERY | EventType.ANONYMOUS_GTID, _, _), _) =>
          (transactionState, None)

        case _ =>
          (transactionState, None)
      }
    }

  def handleCreate(
      tableId: Long,
      offset: Long,
      timestamp: Long,
      rows: List[Array[Serializable]],
      includedColumns: Array[Int]
  )(implicit transactionState: TransactionState): (TransactionState, Option[TransactionPackage]) = {

    val jsonRows = (for {
      tableName <- toTableName(tableId)
      tableMeta <- transactionState.schemaMetadata.tables.get(tableName)
    } yield rows
      .map(row =>
        convertToJson(
          tableMeta,
          timestamp,
          "create",
          transactionState.fileName,
          transactionState.offset,
          includedColumns,
          (None, Some(nullsToOptions(row)))
        )
      )).getOrElse(Nil)

    (
      transactionState
        .copy(transactionEvents = transactionState.transactionEvents ++ jsonRows, offset = offset),
      None
    )
  }

  def handleUpdate(
      tableId: Long,
      offset: Long,
      timestamp: Long,
      beforeAfter: List[(Array[Serializable], Array[Serializable])],
      includedColumns: Array[Int]
  )(
      implicit transactionState: TransactionState
  ): (TransactionState, Option[TransactionPackage]) = {

    val jsonRows = (for {
      tableName <- toTableName(tableId)
      tableMeta <- transactionState.schemaMetadata.tables.get(tableName)
    } yield beforeAfter
      .map { case (before, after) =>
        convertToJson(
          tableMeta,
          timestamp,
          "update",
          transactionState.fileName,
          offset,
          includedColumns,
          (Some(nullsToOptions(before)), Some(nullsToOptions(after)))
        )
      }).getOrElse(Nil)

    (
      transactionState.copy(
        transactionEvents = transactionState.transactionEvents ++ jsonRows,
        offset = offset,
        timestamp = timestamp
      ),
      None
    )
  }

  def handleDelete(
      tableId: Long,
      offset: Long,
      timestamp: Long,
      rows: List[Array[Serializable]],
      includedColumns: Array[Int]
  )(
      implicit transactionState: TransactionState
  ): (TransactionState, Option[TransactionPackage]) = {
    val jsonRows = (for {
      tableName <- toTableName(tableId)
      tableMeta <- transactionState.schemaMetadata.tables.get(tableName)
    } yield rows
      .map(row =>
        convertToJson(
          tableMeta,
          timestamp,
          "delete",
          transactionState.fileName,
          transactionState.offset,
          includedColumns,
          (Some(nullsToOptions(row)), None)
        )
      )).getOrElse(Nil)
    (
      transactionState.copy(
        transactionEvents = transactionState.transactionEvents ++ jsonRows,
        offset = offset,
        timestamp = timestamp
      ),
      None
    )
  }

  def handleDdl(
      table: String,
      timestamp: Long,
      offset: Long,
      sqlAction: String
  )(
      implicit transactionState: TransactionState
  ): (TransactionState, Option[TransactionPackage]) = {
    val ddlEvent = Queue(
      EventMessage(
        table,
        timestamp,
        sqlAction,
        None,
        transactionState.fileName,
        offset,
        endOfTransaction = true,
        Json.Null,
        Json.Null
      )
    )

    val pack = transactionState
      .copy(
        offset = offset,
        end = timestamp,
        transactionEvents = transactionState.transactionEvents ++ ddlEvent
      )
      .assemblePackage
    (
      TransactionState(
        schemaMetadata = transactionState.schemaMetadata,
        offset = offset,
        timestamp = timestamp,
        fileName = transactionState.fileName,
        transactionEvents = Queue.empty
      ),
      Some(pack)
    )
  }

  def handleCommit(
      transactionState: TransactionState,
      offset: Long,
      timestamp: Long,
      xaId: Option[Long]
  ): (TransactionState, Option[TransactionPackage]) = {
    val marked = (transactionState.transactionEvents match {
      case xs :+ x => xs :+ x.copy(endOfTransaction = true, offset = offset)
      case xs      => xs
    }).map(_.copy(xaId = xaId))

    val pack = transactionState
      .copy(offset = offset, end = timestamp, transactionEvents = marked)
      .assemblePackage
    (
      TransactionState(
        schemaMetadata = transactionState.schemaMetadata,
        offset = offset,
        timestamp = timestamp,
        fileName = transactionState.fileName,
        transactionEvents = Queue.empty
      ),
      Some(pack)
    )
  }

  def convertToJson(
      tableMeta: TableMetadata,
      timestamp: Long,
      action: String,
      fileName: String,
      offset: Long,
      includedColumns: Array[Int],
      record: (Option[Row], Option[Row])
  ): EventMessage = {

    val ba = record match {
      case (Some(before), Some(after)) =>
        List(
          ("before", Json.fromFields(recordToJson(tableMeta, includedColumns, before))),
          ("after", Json.fromFields(recordToJson(tableMeta, includedColumns, after)))
        )
      case (None, Some(after)) =>
        List(
          ("before", Json.Null),
          ("after", Json.fromFields(recordToJson(tableMeta, includedColumns, after)))
        )
      case (Some(before), None) =>
        List(
          ("before", Json.fromFields(recordToJson(tableMeta, includedColumns, before))),
          ("after", Json.Null)
        )
      case _ => List(("before", Json.Null), ("after", Json.Null))
    }

    val pk = record match {
      case (_, Some(after)) =>
        Json.fromFields(extractPk(tableMeta, includedColumns, after))
      case (Some(before), None) =>
        Json.fromFields(extractPk(tableMeta, includedColumns, before))
      case _ => Json.Null
    }
    EventMessage(
      table = tableMeta.name,
      timestamp = timestamp,
      action = action,
      xaId = None,
      fileName = fileName,
      offset = offset,
      endOfTransaction = false,
      row = Json.fromFields(ba),
      pk = pk
    )
  }

  def extractPk(
      tableMeta: TableMetadata,
      columns: Array[Int],
      row: Array[Option[Serializable]]
  ): Array[(String, Json)] =
    columns
      .map(i => tableMeta.columns(i + 1))
      .zip(row)
      .filter { case (meta, _) => meta.isPk }
      .map(mapRawToMeta(tableMeta))

  def toTableName(tableId: Long)(implicit transactionState: TransactionState): Option[String] =
    transactionState.schemaMetadata.idToTable.get(tableId).map(_.name)

  def recordToJson(
      tableMetadata: TableMetadata,
      includedColumns: Array[Int],
      record: Array[Option[Serializable]]
  ): Iterable[(String, Json)] =
    includedColumns
      .map(i => tableMetadata.columns(i + 1))
      .zip(record)
      .map(mapRawToMeta(tableMetadata))

  def mapRawToMeta(table: TableMetadata): ((ColumnMetadata, Option[Serializable])) => (String, Json) = {
    case (column, Some(value)) =>
      /* At some point, Scala will drop ClassTag, so we should be prepared to either drop cross compilation for 2.13,
       * else add an alternative implementation that uses TypeTest.  At the current time, I don't want to introduce
       * any potential bugs into _this_ at the same time that I'm introducing Scala 3.. */
      @annotation.nowarn(
        "msg=Use of `scala.reflect.ClassTag` for type testing may be unsound. Consider using `scala.reflect.TypeTest` instead."
      )
      def unsafeCast[T](implicit m: ClassTag[T]): T =
        try
          value match {
            case _: T => value.asInstanceOf[T]
            case _    => m.runtimeClass.cast(value).asInstanceOf[T]
          }
        catch {
          case e: ClassCastException =>
            throw new IllegalStateException(
              s"""Unable to process column [schema:${table.schema}, table:${table.name}, column:${column.name}]. Failed to convert a mysql '${column.dataType}' to a ${m.runtimeClass.getCanonicalName}, but got: ${e.getMessage}""",
              e
            )
        }

      val jsonValue = column.dataType match {
        case "bigint"                     => Json.fromLong(unsafeCast[Long])
        case "int" | "tinyint"            => Json.fromInt(unsafeCast[Int])
        case "date" | "datetime" | "time" => Json.fromLong(unsafeCast[Long])
        case "decimal"                    => Json.fromBigDecimal(unsafeCast[BigDecimal])
        case "float"                      => Json.fromFloat(unsafeCast[Float]).get
        case "text" | "mediumtext" | "longtext" | "tinytext" | "varchar" | "char" =>
          Json.fromString(new String(unsafeCast[Array[Byte]]))
        case "json" => Json.fromString(JsonBinary.parseAsString(unsafeCast[Array[Byte]]))
        case _      => Json.fromString(value.toString)
      }

      column.name -> jsonValue
    case (metadata, _) => metadata.name -> Json.Null
  }

  def nullsToOptions(row: Array[Serializable]): Row = row.map(Option(_))

  def createTransactionState[F[_]: Sync: Logger](
      schemaMetadata: SchemaMetadata,
      binlogClient: BinaryLogClient
  ): F[Ref[F, TransactionState]] =
    Ref[F]
      .of(
        TransactionState(
          Queue.empty,
          schemaMetadata = schemaMetadata,
          fileName = binlogClient.getBinlogFilename,
          offset = 0,
          timestamp = 0
        )
      )
      .flatMap(v => Logger[F].info("created transaction state") >> Sync[F].pure(v))

}
