package db
import cats.effect.IO
import com.dimafeng.testcontainers.{ ForAllTestContainer, SingleContainer }
import io.laserdisc.mysql.binlog.config.BinLogConfig
import org.testcontainers.containers.MySQLContainer

import java.net.URI
import scala.concurrent.ExecutionContext
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

  override val container: SingleContainer[MySQLContainer[_]] =
    new SingleContainer[MySQLContainer[_]] {
      implicit override val container: MySQLContainer[_] = mySqlContainer
    }

  implicit val ec: ExecutionContext = Implicits.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  def containerBinlogConfig: BinLogConfig = {

    val cleanURI = mySqlContainer.getJdbcUrl.substring(5)
    val uri      = URI.create(cleanURI)

    BinLogConfig(
      uri.getHost,
      uri.getPort,
      mySqlContainer.getUsername,
      mySqlContainer.getPassword,
      mySqlContainer.getDatabaseName,
      useSSL = false,
      poolSize = 3
    )
  }
}
