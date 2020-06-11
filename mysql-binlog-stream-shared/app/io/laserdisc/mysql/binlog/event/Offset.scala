package io.laserdisc.mysql.binlog.event

trait Offset extends Ordered[Offset] {

  val fileName: String
  val offset: Long

  override def compare(that: Offset): Int = OffsetOrdering.ordering.compare(this, that)
}

object OffsetOrdering {

  implicit def ordering[A <: Offset]: Ordering[A] =
    new Ordering[A] {
      override def compare(x: A, y: A): Int =
        f"${x.fileName}${x.offset}%015d".compareTo(f"${y.fileName}${y.offset}%015d")
    }
}
