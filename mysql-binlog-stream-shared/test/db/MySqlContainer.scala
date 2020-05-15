package db
import cats.effect.IO
import com.dimafeng.testcontainers.{ ForAllTestContainer, SingleContainer }
import org.testcontainers.containers.MySQLContainer

import scala.concurrent.ExecutionContext.Implicits
import scala.language.existentials

trait MySqlContainer {
  this: ForAllTestContainer =>
  type OTCContainer = MySQLContainer[T] forSome {
    type T <: MySQLContainer[T]
  }
  val mySqlContainer: OTCContainer = new MySQLContainer("mysql:5.7").withUsername("root")
  mySqlContainer.withCommand("mysqld --log-bin --server-id=1 --binlog-format=ROW")
  mySqlContainer.withInitScript("init.sql")
  mySqlContainer.withPassword("")

  override val container = new SingleContainer[MySQLContainer[_]] {
    implicit override val container: MySQLContainer[_] = mySqlContainer
  }

  implicit val ec = Implicits.global
  implicit val cs = IO.contextShift(ec)

}
