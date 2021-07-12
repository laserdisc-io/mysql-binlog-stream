package io.laserdisc.mysql.binlog.kinesis.container

import com.dimafeng.testcontainers.GenericContainer
import org.scanamo.LocalDynamoDB
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType._

trait DynamoDBContainer {

  val TestOffsetTableName: String = "test-offsets"

  val ddbContainer: GenericContainer = new GenericContainer(
    "amazon/dynamodb-local",
    waitStrategy = Some(new HttpWaitStrategy().forPath("/shell"))
  ).configure(_.withExposedPorts(8000))

  private[this] def localDDBClient = LocalDynamoDB.client(ddbContainer.mappedPort(8000))

  def containerDDBClient: DynamoDbAsyncClient = localDDBClient

  def createDDBOffsetTestTable(): CreateTableResponse = {
    LocalDynamoDB.createTable(localDDBClient)(TestOffsetTableName)("appName" -> S)
  }
}
