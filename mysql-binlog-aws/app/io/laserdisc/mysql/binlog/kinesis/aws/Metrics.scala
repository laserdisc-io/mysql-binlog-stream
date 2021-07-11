package io.laserdisc.mysql.binlog.kinesis.aws

import cats.data.State
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatch.model._

trait Metrics[F[_]] {

  def publish(newValue: Long): State[Metric, Metric] =
    State { current =>
      val datum = build(current.metricName, newValue, current.env)

      val request: PutMetricDataRequest = PutMetricDataRequest.builder
        .namespace(current.namespace)
        .metricData(datum)
        .build

      Metrics.cw.putMetricData(request)

      val updated = current.copy(value = newValue)
      updated -> updated
    }

  private[this] def build(metricName: String, count: Long, env: String): MetricDatum =
    MetricDatum
      .builder()
      .metricName(metricName)
      .unit(StandardUnit.COUNT)
      .dimensions(
        Dimension
          .builder()
          .name("environment")
          .value(env)
          .build()
      )
      .value(count.toDouble)
      .build()

}
case class Metric(namespace: String, metricName: String, env: String, value: Long)
object Metrics {

  val cw: CloudWatchClient = CloudWatchClient
    .builder()
    .region(Region.US_EAST_1)
    .build();
}
