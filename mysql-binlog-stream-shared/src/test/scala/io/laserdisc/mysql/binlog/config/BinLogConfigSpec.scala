package io.laserdisc.mysql.binlog.config

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class BinLogConfigSpec extends AnyWordSpec with Matchers {

  "BinLogConfig" should {

    val template = BinLogConfig(
      host = "testHost",
      port = 3360,
      user = "testUser",
      password = "testPassword",
      schema = "testSchema",
      useSSL = true,
      driverClass = "com.made.up.TestDriver",
      urlOverride = None,
      poolSize = 3,
      serverId = Some(222)
    )

    "build correct connection URL" in {

      template.connectionURL should equal(
        "jdbc:mysql://testHost:3360/testSchema?useSSL=true"
      )
      template.copy(useSSL = false).connectionURL should equal(
        "jdbc:mysql://testHost:3360/testSchema"
      )
      template.copy(schema = "b", port = 9999).connectionURL should equal(
        "jdbc:mysql://testHost:9999/b?useSSL=true"
      )
      template.copy(urlOverride = Some("the-cat-goes-miaow")).connectionURL should equal(
        "the-cat-goes-miaow"
      )
    }

    "not expose password on toString" in {

      (template.copy(password = "TOPSECRET").toString should not).include("TOPSECRET")

    }

  }

}
