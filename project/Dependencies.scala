import sbt.*
import sbt.Keys.libraryDependencies

object Dependencies {
  val cirisVersion  = "3.15.0"
  val doobieVersion = "1.0.0-RC12"
  val circeVersion  = "0.14.16"

  val TestLib = libraryDependencies ++= Seq(
    "org.scalamock"     %% "scalamock"            % "7.5.5"  % Test,
    "org.scalatest"     %% "scalatest"            % "3.2.20" % Test,
    "com.dimafeng"      %% "testcontainers-scala" % "0.44.1" % Test,
    "org.testcontainers" % "testcontainers-mysql" % "2.0.5"  % Test,
    "org.testcontainers" % "testcontainers"       % "2.0.5"  % Test
  )

  val Config = libraryDependencies ++= Seq(
    "is.cir"     %% "ciris-enumeratum" % cirisVersion,
    "is.cir"     %% "ciris-refined"    % cirisVersion,
    "eu.timepit" %% "refined"          % "0.11.4"
  )

  val Logging = libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % "1.5.37",
    "ch.qos.logback" % "logback-core"    % "1.5.37",
    "org.slf4j"      % "jcl-over-slf4j"  % "2.0.18",
    "org.slf4j"      % "jul-to-slf4j"    % "2.0.18",
    "org.typelevel" %% "log4cats-slf4j"  % "2.8.0"
  )

  val Persistence = libraryDependencies ++= Seq(
    "org.tpolecat" %% "doobie-core"                 % doobieVersion,
    "org.tpolecat" %% "doobie-hikari"               % doobieVersion,
    "org.tpolecat" %% "doobie-refined"              % doobieVersion,
    "org.tpolecat" %% "doobie-scalatest"            % doobieVersion % Test,
    "com.mysql"     % "mysql-connector-j"           % "26.7.0",
    "com.zendesk"   % "mysql-binlog-connector-java" % "0.30.3"
  )

  val Circe = libraryDependencies ++= Seq(
    "io.circe" %% "circe-core"   % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-optics" % "0.15.1" % Test
  )

  val XML = libraryDependencies ++= Seq(
    "javax.xml.bind"   % "jaxb-api"   % "2.3.1",
    "com.sun.xml.bind" % "jaxb-impl"  % "4.0.9",
    "com.sun.xml.bind" % "jaxb-core"  % "4.0.9",
    "javax.activation" % "activation" % "1.1.1"
  )

}
