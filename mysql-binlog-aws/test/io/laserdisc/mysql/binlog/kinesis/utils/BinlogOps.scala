package io.laserdisc.mysql.binlog.kinesis.utils

import com.dimafeng.testcontainers.SingleContainer
import org.testcontainers.containers.MySQLContainer

object BinlogOps {
  def toSingleContainer(c: MySQLContainer[_]): SingleContainer[MySQLContainer[_]] =
    new SingleContainer[MySQLContainer[_]] {
      implicit override val container: MySQLContainer[_] = c
    }
}
