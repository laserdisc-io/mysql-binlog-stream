import sbt.*
import sbt.Keys.libraryDependencies

object Dependencies {
  val cirisVersion      = "3.6.0"
  val doobieVersion     = "1.0.0-RC6"
  val circeVersion      = "0.14.7"
  val catsEffectVersion = "3.5.4"

  val TestLib = Seq(
    libraryDependencies ++= Seq(
      "org.scalamock"     %% "scalamock"            % "6.0.0"  % Test,
      "org.scalatest"     %% "scalatest"            % "3.2.18" % Test,
      "com.dimafeng"      %% "testcontainers-scala" % "0.41.3" % Test,
      "org.testcontainers" % "mysql"                % "1.19.8" % Test,
      "org.testcontainers" % "testcontainers"       % "1.19.8" % Test
    )
  )

  val Config = Seq(
    libraryDependencies ++= Seq(
      "is.cir"     %% "ciris-enumeratum" % cirisVersion,
      "is.cir"     %% "ciris-refined"    % cirisVersion,
      "eu.timepit" %% "refined"          % "0.11.1"
    )
  )

  val Logging = Seq(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.5.6",
      "ch.qos.logback" % "logback-core"    % "1.5.6",
      "org.slf4j"      % "jcl-over-slf4j"  % "2.0.13",
      "org.slf4j"      % "jul-to-slf4j"    % "2.0.13",
      "org.typelevel" %% "log4cats-slf4j"  % "2.7.0"
    )
  )

  val Persistence = Seq(
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core"                 % doobieVersion,
      "org.tpolecat" %% "doobie-hikari"               % doobieVersion,
      "org.tpolecat" %% "doobie-refined"              % doobieVersion,
      "org.tpolecat" %% "doobie-scalatest"            % doobieVersion % Test,
      "mysql"         % "mysql-connector-java"        % "8.0.33",
      "com.zendesk"   % "mysql-binlog-connector-java" % "0.29.2"
    )
  )

  val Circe = Seq(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core"   % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-optics" % "0.15.0" % Test
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
      "com.sun.xml.bind" % "jaxb-impl"  % "4.0.5",
      "com.sun.xml.bind" % "jaxb-core"  % "4.0.5",
      "javax.activation" % "activation" % "1.1.1"
    )
  )
}
