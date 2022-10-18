package io.laserdisc.mysql.binlog

import com.github.shyiko.mysql.binlog.BinaryLogClient
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer.CompatibilityMode
import com.github.shyiko.mysql.binlog.network.SSLMode
import io.laserdisc.mysql.binlog.checkpoint.BinlogOffset

package object config {

  implicit class BinLogConfigOps(val v: BinLogConfig) extends AnyVal {

    def mkBinaryLogClient(offset: Option[BinlogOffset] = None): BinaryLogClient = {

      val blc = new BinaryLogClient(v.host, v.port, v.user, v.password)

      blc.setEventDeserializer {
        val ed = new EventDeserializer()
        ed.setCompatibilityMode(
          CompatibilityMode.DATE_AND_TIME_AS_LONG_MICRO,
          CompatibilityMode.CHAR_AND_BINARY_AS_BYTE_ARRAY
        )
        ed
      }

      blc.setSSLMode(if (v.useSSL) SSLMode.VERIFY_IDENTITY else SSLMode.DISABLED)

      offset match {
        case Some(o) =>
          blc.setBinlogFilename(o.fileName)
          blc.setBinlogPosition(o.offset)
          blc
        case _ =>
          blc
      }

    }

  }

}
