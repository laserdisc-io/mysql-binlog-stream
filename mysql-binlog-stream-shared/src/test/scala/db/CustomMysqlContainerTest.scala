package db

import com.dimafeng.testcontainers.*
import org.testcontainers.containers.{GenericContainer as JavaGenericContainer, Network}
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.lifecycle.Startable
import org.testcontainers.utility.DockerImageName

import java.nio.file.Path
import scala.jdk.CollectionConverters.*

trait CustomMysqlContainerTest { this: ForAllTestContainer =>

//  protected val AppPort = 9000

  protected val sharedNetwork: Network = Network.newNetwork()

  protected lazy val mySQLContainer: MySQLContainer = new MySQLContainer(
    mysqlImageVersion = Some(DockerImageName.parse("mysql:8.0"))
  ) {
    container.withNetwork(sharedNetwork)
    container.withNetworkAliases("mysql")
    container.withEnv("ENVIRONMENT", "local")
  }

  protected def baseAppContainer(
      name: String,
      jarName: String,
      mainClass: String,
      baseFolder: String,
      port: Int = 333,
      // waitStrategy: WaitStrategy = WaitStrategyForAPI,
      envVars: Map[String, String] = Map.empty,
      containerDependencies: List[Startable]
  ): GenericContainer = new GenericContainer({
    val c = new JavaGenericContainer(
      new ImageFromDockerfile()
        .withFileFromPath(jarName, Path.of(s"$baseFolder/$jarName").toAbsolutePath)
        .withDockerfileFromBuilder { builder =>
          builder
            .from("openjdk:11-jre-slim")
            .copy(jarName, s"/$jarName")
            .cmd("java", "-Xmx256m", "-cp", s"/$jarName", mainClass)
            .build()
        }
    )
    c.dependsOn(containerDependencies.asJavaCollection)
    c.withEnv(envVars.asJava)
    // c.withStartupAttempts(3)
    c.withEnv("ENVIRONMENT", "local")
    c.withEnv("APP_PORT", port.toString)
    c.withEnv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")
    // c.setWaitStrategy(waitStrategy)
    c.withExposedPorts(port)
    c.withNetwork(sharedNetwork)
    c.withNetworkAliases(name)

    c
  })

}
