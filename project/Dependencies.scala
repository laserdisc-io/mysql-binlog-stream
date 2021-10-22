import sbt.Keys.{ libraryDependencies, _ }
import sbt._

object Dependencies {
  val cirisVersion  = "1.2.1"
  val doobieVersion = "0.13.4"
  val circeVersion  = "0.14.1"
  val catsVersion   = "2.5.4"

  val TestLib = Seq(
    libraryDependencies ++= Seq(
      "org.scalamock"     %% "scalamock"            % "5.1.0"  % Test,
      "org.scalatest"     %% "scalatest"            % "3.2.10" % Test,
      "com.dimafeng"      %% "testcontainers-scala" % "0.39.9" % Test,
      "org.testcontainers" % "mysql"                % "1.16.2" % Test,
      "org.testcontainers" % "testcontainers"       % "1.16.2" % Test
    )
  )

  val Config = Seq(
    libraryDependencies ++= Seq(
      "is.cir"     %% "ciris-enumeratum" % cirisVersion,
      "is.cir"     %% "ciris-refined"    % cirisVersion,
      "eu.timepit" %% "refined"          % "0.9.27"
    )
  )

  val Logging = Seq(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.6",
      "ch.qos.logback" % "logback-core"    % "1.2.6",
      "org.slf4j"      % "jcl-over-slf4j"  % "1.7.32",
      "org.slf4j"      % "jul-to-slf4j"    % "1.7.32",
      "org.typelevel" %% "log4cats-slf4j"  % "1.3.1"
    )
  )

  val Persistence = Seq(
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core"                 % doobieVersion,
      "org.tpolecat" %% "doobie-hikari"               % doobieVersion, // HikariCP transactor.
      "org.tpolecat" %% "doobie-refined"              % doobieVersion, // Postgres driver 42.1.4 + type mappings.
      "org.tpolecat" %% "doobie-scalatest"            % doobieVersion % Test, // Support for doobie scalatest
      "mysql"         % "mysql-connector-java"        % "8.0.27",
      "com.zendesk"   % "mysql-binlog-connector-java" % "0.25.4"
    )
  )

  val Circe = Seq(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core"    % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser"  % circeVersion,
      "io.circe" %% "circe-optics"  % circeVersion % Test
    )
  )

  val Cats = Seq(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsVersion
    )
  )

  val XML = Seq(
    libraryDependencies ++= Seq(
      "javax.xml.bind"   % "jaxb-api"   % "2.3.1",
      "com.sun.xml.bind" % "jaxb-impl"  % "3.0.2",
      "com.sun.xml.bind" % "jaxb-core"  % "3.0.2",
      "javax.activation" % "activation" % "1.1.1"
    )
  )
}
