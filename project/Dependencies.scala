import sbt.Keys.{libraryDependencies, _}
import sbt._

object Dependencies {
  val cirisVersion      = "2.4.0"
  val doobieVersion     = "1.0.0-RC2"
  val circeVersion      = "0.14.1"
  val catsEffectVersion = "3.3.14"

  val TestLib = Seq(
    libraryDependencies ++= Seq(
      "org.scalamock"     %% "scalamock"            % "5.2.0"   % Test,
      "org.scalatest"     %% "scalatest"            % "3.2.14"  % Test,
      "com.dimafeng"      %% "testcontainers-scala" % "0.40.15" % Test,
      "org.testcontainers" % "mysql"                % "1.17.5"  % Test,
      "org.testcontainers" % "testcontainers"       % "1.17.5"  % Test
    )
  )

  val Config = Seq(
    libraryDependencies ++= Seq(
      "is.cir"     %% "ciris-enumeratum" % cirisVersion,
      "is.cir"     %% "ciris-refined"    % cirisVersion,
      "eu.timepit" %% "refined"          % "0.10.1"
    )
  )

  val Logging = Seq(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.4.4",
      "ch.qos.logback" % "logback-core"    % "1.4.4",
      "org.slf4j"      % "jcl-over-slf4j"  % "2.0.3",
      "org.slf4j"      % "jul-to-slf4j"    % "2.0.3",
      "org.typelevel" %% "log4cats-slf4j"  % "2.5.0"
    )
  )

  val Persistence = Seq(
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core"                 % doobieVersion,
      "org.tpolecat" %% "doobie-hikari"               % doobieVersion, // HikariCP transactor.
      "org.tpolecat" %% "doobie-refined"              % doobieVersion, // Postgres driver 42.1.4 + type mappings.
      "org.tpolecat" %% "doobie-scalatest"            % doobieVersion % Test, // Support for doobie scalatest
      "mysql"         % "mysql-connector-java"        % "8.0.30",
      "com.zendesk"   % "mysql-binlog-connector-java" % "0.27.3"
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
      "org.typelevel" %% "cats-effect" % catsEffectVersion
    )
  )

  val XML = Seq(
    libraryDependencies ++= Seq(
      "javax.xml.bind"   % "jaxb-api"   % "2.3.1",
      "com.sun.xml.bind" % "jaxb-impl"  % "4.0.1",
      "com.sun.xml.bind" % "jaxb-core"  % "4.0.1",
      "javax.activation" % "activation" % "1.1.1"
    )
  )
}
