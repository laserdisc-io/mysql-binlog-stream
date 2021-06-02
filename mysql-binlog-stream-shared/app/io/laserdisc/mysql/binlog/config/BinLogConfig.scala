package io.laserdisc.mysql.binlog.config

case class BinLogConfig(
  host: String,
  port: Int = 3306,
  user: String,
  password: String,
  schema: String,
  useSSL: Boolean = true,
  driverClass: String = "com.mysql.cj.jdbc.Driver",
  urlOverride: Option[String] = None,
  poolSize: Int
) {
  def connectionURL: String =
    urlOverride.getOrElse(s"jdbc:mysql://$host:$port/$schema${if (useSSL) "?useSSL=true" else ""}")

}
