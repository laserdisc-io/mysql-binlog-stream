package io.laserdisk.mysql.binlog.database

case class DbConfig(
  user: String,
  password: String,
  url: String,
  poolSize: Int,
  className: String = "com.mysql.cj.jdbc.Driver"
)
