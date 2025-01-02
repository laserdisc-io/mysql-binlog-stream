import sbt._
import sbt.Keys.libraryDependencies

object Dependencies {
  val cirisVersion  = "3.7.0"
  val doobieVersion = "1.0.0-RC6"
  val circeVersion  = "0.14.10"

  val TestLib = libraryDependencies ++= Seq(
    "org.scalamock"     %% "scalamock"            % "6.0.0"  % Test,
    "org.scalatest"     %% "scalatest"            % "3.2.19" % Test,
    "com.dimafeng"      %% "testcontainers-scala" % "0.41.4" % Test,
    "org.testcontainers" % "mysql"                % "1.20.4" % Test,
    "org.testcontainers" % "testcontainers"       % "1.20.4" % Test
  )

  val Config = libraryDependencies ++= Seq(
    "is.cir"     %% "ciris-enumeratum" % cirisVersion,
    "is.cir"     %% "ciris-refined"    % cirisVersion,
    "eu.timepit" %% "refined"          % "0.11.2"
  )

  val Logging = libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % "1.5.12",
    "ch.qos.logback" % "logback-core"    % "1.5.12",
    "org.slf4j"      % "jcl-over-slf4j"  % "2.0.16",
    "org.slf4j"      % "jul-to-slf4j"    % "2.0.16",
    "org.typelevel" %% "log4cats-slf4j"  % "2.7.0"
  )

  val Persistence = libraryDependencies ++= Seq(
    "org.tpolecat" %% "doobie-core"                 % doobieVersion,
    "org.tpolecat" %% "doobie-hikari"               % doobieVersion,
    "org.tpolecat" %% "doobie-refined"              % doobieVersion,
    "org.tpolecat" %% "doobie-scalatest"            % doobieVersion % Test,
    "mysql"         % "mysql-connector-java"        % "8.0.33",
    "com.zendesk"   % "mysql-binlog-connector-java" % "0.30.1"
  )

  val Circe = libraryDependencies ++= Seq(
    "io.circe" %% "circe-core"   % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-optics" % "0.15.0" % Test
  )

//  val Cats = libraryDependencies ++= Seq("org.typelevel" %% "cats-effect" % "3.5.6")

  val XML = libraryDependencies ++= Seq(
    "javax.xml.bind"   % "jaxb-api"   % "2.3.1",
    "com.sun.xml.bind" % "jaxb-impl"  % "4.0.5",
    "com.sun.xml.bind" % "jaxb-core"  % "4.0.5",
    "javax.activation" % "activation" % "1.1.1"
  )

}
