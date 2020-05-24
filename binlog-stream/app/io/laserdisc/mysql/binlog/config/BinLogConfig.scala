package io.laserdisc.mysql.binlog.config

case class BinLogConfig(
  mojoDbHost: String,
  dbUser: String,
  dbPassword: String,
  schema: String,
  port: Int = 3306,
  useSSL: Boolean = true
)
