package db

import com.dimafeng.testcontainers.*
import com.dimafeng.testcontainers.scalatest.TestContainersForAll
import io.laserdisc.mysql.binlog.config.BinLogConfig
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.testcontainers.containers.Network
import org.testcontainers.utility.DockerImageName

import java.net.URI
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits
import scala.jdk.CollectionConverters.*

trait MySqlContainerTest extends TestContainersForAll with BeforeAndAfterEach {
  self: Suite =>

  override type Containers = MySQLContainer

  def startContainers() = {
    container.start()
    container
  }

  def container = mySQLContainer

  protected val sharedNetwork: Network = Network.newNetwork()

  protected lazy val mySQLContainer: MySQLContainer = new MySQLContainer(
    mysqlImageVersion = Some(System.getProperty("os.arch") match {
      case "aarch64" => DockerImageName.parse("biarms/mysql:5.7").asCompatibleSubstituteFor("mysql")
      case _         => DockerImageName.parse("mysql:5.7")
    })
  ) {
    this.container.withNetwork(sharedNetwork)
    this.container.withNetworkAliases("mysql")
    this.container.withEnv("ENVIRONMENT", "local")
    this.container.withCommand("mysqld --log-bin --server-id=1 --binlog-format=ROW --explicit_defaults_for_timestamp=1")
    this.container.withTmpFs(Map("/var/lib/mysql" -> "rw").asJava)
    this.container.withUsername("root")
    this.container.withPassword("")
    this.container.withInitScript("init.sql")

  }
  protected lazy val mysqlDBCreds = {
    val props = new java.util.Properties()
    props.put("user", container.username)
    props.put("password", container.password)
    props
  }

  implicit val ec: ExecutionContext = Implicits.global

  def binlogConfig: BinLogConfig = {

    val cleanURI = mySQLContainer.jdbcUrl.substring(5)
    val uri      = URI.create(cleanURI)

    BinLogConfig(
      host = uri.getHost,
      port = uri.getPort,
      user = mySQLContainer.username,
      password = mySQLContainer.password,
      schema = mySQLContainer.databaseName,
      useSSL = false,
      poolSize = 3,
      serverId = Some(1234)
    )
  }

}
