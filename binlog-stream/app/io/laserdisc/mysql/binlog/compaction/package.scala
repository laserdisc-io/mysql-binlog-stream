package io.laserdisc.mysql.binlog

import cats.data.Kleisli
import io.circe.Json
import io.laserdisc.mysql.binlog.event.EventMessage

import scala.collection.mutable
import cats.implicits._

package object compaction {
  def compact(transaction: Seq[EventMessage]): Seq[EventMessage] =
    transaction
      .foldLeft(mutable.LinkedHashMap.empty[Json, EventMessage]) {
        case (acc, evt) =>
          acc.get(evt.pk).fold[Unit](acc.put(evt.pk, evt)) { latest =>
            mkNewEvent(evt)
              .andThen(finalizeNewEvent)
              .run(latest) match {
              case Some(ne) =>
                acc.remove(evt.pk);
                acc.put(evt.pk, ne)
              case None => acc.remove(evt.pk)
            }
          }
          //TODO: use this code, once Scala 2.12 support ends, or  updateWith back ported to 2.12A
//          acc.updateWith(evt.pk) {
//            case Some(latest) =>
//              mkNewEvent(evt).andThen(finalizeNewEvent).run(latest)
//            case None => Some(evt)
//          }
          acc
      }
      .values
      .toSeq

  def mkNewEvent(current: EventMessage): Kleisli[Option, EventMessage, EventMessage] =
    Kleisli(latest =>
      latest.row.hcursor
        .downField("after")
        .set(current.row.hcursor.downField("after").focus.getOrElse(Json.Null))
        .top
        .map(newJson =>
          latest
            .copy(
              timestamp = current.timestamp,
              fileName = current.fileName,
              offset = current.offset,
              row = newJson
            )
        )
    )

  val finalizeNewEvent: Kleisli[Option, EventMessage, EventMessage] = Kleisli(value =>
    (value.row.hcursor.downField("before").focus, value.row.hcursor.downField("after").focus)
      .mapN[Option[EventMessage]] {
        case (Json.Null, Json.Null) => None
        case (Json.Null, _)         => Some(value.copy(action = "create"))
        case (_, Json.Null)         => Some(value.copy(action = "delete"))
        case (_, _)                 => Some(value.copy(action = "update"))
      }
      .flatten
  )
}
