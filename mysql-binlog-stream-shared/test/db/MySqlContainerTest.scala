package db

import com.dimafeng.testcontainers.{ForAllTestContainer, SingleContainer}
import io.laserdisc.mysql.binlog.config.BinLogConfig
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

import java.net.URI
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.language.existentials

trait MySqlContainerTest {
  this: ForAllTestContainer =>
  type OTCContainer = MySQLContainer[T] forSome {
    type T <: MySQLContainer[T]
  }

  val mySqlContainer: OTCContainer = new MySQLContainer(DockerImageName.parse("mysql:5.7"))
  mySqlContainer.withCommand("mysqld --log-bin --server-id=1 --binlog-format=ROW --explicit_defaults_for_timestamp=1")
  mySqlContainer.withTmpFs(Map("/var/lib/mysql" -> "rw").asJava)
  mySqlContainer.withUsername("root")
  mySqlContainer.withPassword("")
  mySqlContainer.withInitScript("init.sql")

  override val container: SingleContainer[MySQLContainer[_]] =
    new SingleContainer[MySQLContainer[_]] {
      override implicit val container: MySQLContainer[_] = mySqlContainer
    }

  implicit val ec: ExecutionContext = Implicits.global

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
      poolSize = 3,
      serverId = Some(1234)
    )
  }
}
