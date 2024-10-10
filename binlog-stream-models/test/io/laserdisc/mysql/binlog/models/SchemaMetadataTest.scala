package io.laserdisc.mysql.binlog.models

import cats.effect.IO
import com.dimafeng.testcontainers.ForAllTestContainer
import db.MySqlContainer
import doobie.util.transactor.Transactor.Aux
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import cats.effect.unsafe.implicits.global
import doobie.util.transactor.Transactor

import java.util.Properties

class SchemaMetadataTest
    extends AnyWordSpec
    with ForAllTestContainer
    with MySqlContainer
    with Matchers {

  "Schema Metadata" should {

    "restore schema state from DB" in {
      implicit val testTransactor: Aux[IO, Unit] =
        Transactor.fromDriverManager(
          driver = mySqlContainer.getDriverClassName,
          url = s"${mySqlContainer.getJdbcUrl}?useSSL=false",
          info = {
            // ugh
            val p = new Properties()
            p.put("user", mySqlContainer.getUsername)
            p.put("password", mySqlContainer.getPassword)
            p
          },
          None
        )
      val schemaState =
        SchemaMetadata.buildSchemaMetadata("test").unsafeRunSync()

      schemaState.tables                should have size 2
      schemaState.tables("sku").columns should have size 2
      schemaState
        .tables("sku")
        .columns
        .values
        .filter(_.isPk)
        .head
        .name                               should be("id")
      schemaState.tables("variant").columns should have size 2
      schemaState
        .tables("variant")
        .columns
        .values
        .filter(_.isPk)
        .head
        .name should be("id")
    }
  }

}
