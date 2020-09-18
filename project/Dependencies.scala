import sbt.Keys.{ libraryDependencies, _ }
import sbt._

object Dependencies {
  val AwsSdkVersion    = "1.11.864"
  val cirisVersion     = "1.2.1"
  val doobieVersion    = "0.9.2"
  val jdbcdslogVersion = "1.0.6.2"
  val circeVersion     = "0.13.0"
  val Http4sVersion    = "0.20.19"
  val fs2AwsVersion    = "3.0.1"
  val fs2JmsVersion    = "0.0.2"
  val cormorantVersion = "0.2.0-M2"
  val sshjVersion      = "0.26.0"
  val ScanamoVersion   = "1.0.0-M12-1"

  val TestLib = Seq(
    libraryDependencies ++= Seq(
      "io.github.sullis"  %% "jms-testkit"          % "0.2.8"       % Test, // ApacheV2
      "org.scalamock"     %% "scalamock"            % "5.0.0"       % Test,
      "org.scalatest"     %% "scalatest"            % "3.2.2"       % Test, // ApacheV2
      "com.dimafeng"      %% "testcontainers-scala" % "0.38.3"      % Test,
      "org.testcontainers" % "mysql"                % "1.14.3"      % Test,
      "org.mockito"        % "mockito-core"         % "3.5.7"       % Test,
      "io.laserdisc"      %% "fs2-aws-testkit"      % fs2AwsVersion % Test,
      "com.dimafeng"      %% "testcontainers-scala" % "0.35.0"      % Test
    )
  )

  val Kinesis = Seq(
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-java-sdk-kinesis"  % AwsSdkVersion excludeAll ("commons-logging", "commons-logging"), // ApacheV2
      "com.amazonaws" % "amazon-kinesis-client" % "1.9.0" excludeAll ("commons-logging", "commons-logging") // Amazon Software License
    )
  )

  val Dynamo = Seq(
    libraryDependencies ++= Seq("org.scanamo" %% "scanamo-cats-effect" % ScanamoVersion)
  )

  val fs2_aws = Seq(
    libraryDependencies ++= Seq(
      "io.laserdisc" %% "fs2-aws"         % fs2AwsVersion excludeAll ("commons-logging", "commons-logging"),
      "io.laserdisc" %% "fs2-aws-testkit" % fs2AwsVersion % Test excludeAll ("commons-logging", "commons-logging")
    )
  )

  val Sqs = Seq(
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-java-sdk-sqs"              % AwsSdkVersion excludeAll ("commons-logging", "commons-logging"),
      "com.amazonaws" % "amazon-sqs-java-messaging-lib" % "1.0.8" excludeAll ("commons-logging", "commons-logging")
    )
  )

  val Ssm = Seq(
    libraryDependencies += "com.amazonaws" % "aws-java-sdk-ssm" % AwsSdkVersion excludeAll ("commons-logging", "commons-logging")
  )

  val Config = Seq(
    libraryDependencies ++= Seq(
//      "is.cir"     %% "ciris-core"       % cirisVersion,
      "is.cir"     %% "ciris-enumeratum" % "1.2.1",
      "is.cir"     %% "ciris-refined"    % cirisVersion,
      "eu.timepit" %% "refined"          % "0.9.15"
    )
  )

  val Logging = Seq(
    libraryDependencies ++= Seq(
      "com.amazonaws"      % "aws-java-sdk-cloudwatch" % AwsSdkVersion excludeAll ("commons-logging", "commons-logging"),
      "ch.qos.logback"     % "logback-classic"         % "1.2.3", // logging
      "ch.qos.logback"     % "logback-core"            % "1.2.3", // logging
      "org.slf4j"          % "jcl-over-slf4j"          % "1.7.30",
      "org.slf4j"          % "jul-to-slf4j"            % "1.7.30",
      "io.chrisdavenport" %% "log4cats-slf4j"          % "1.1.1"
    )
  )

  val Persistence = Seq(
    libraryDependencies ++= Seq(
      "org.tpolecat"      %% "doobie-core"                 % doobieVersion,
      "org.tpolecat"      %% "doobie-hikari"               % doobieVersion, // HikariCP transactor.
      "org.tpolecat"      %% "doobie-refined"              % doobieVersion, // Postgres driver 42.1.4 + type mappings.
      "org.tpolecat"      %% "doobie-scalatest"            % doobieVersion, // Support for doobie scalatest
      "com.googlecode.usc" % "jdbcdslog"                   % jdbcdslogVersion,
      "mysql"              % "mysql-connector-java"        % "8.0.21",
      "com.github.shyiko"  % "mysql-binlog-connector-java" % "0.21.0"
    )
  )
  val Http4s = Seq(
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s" %% "http4s-circe"        % Http4sVersion,
      "org.http4s" %% "http4s-dsl"          % Http4sVersion,
      "org.http4s" %% "http4s-client"       % Http4sVersion
    )
  )
  val Circe = Seq(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core"           % circeVersion,
      "io.circe" %% "circe-generic"        % circeVersion,
      "io.circe" %% "circe-generic-extras" % "0.13.0",
      "io.circe" %% "circe-parser"         % circeVersion,
      "io.circe" %% "circe-refined"        % circeVersion,
      "io.circe" %% "circe-optics"         % "0.13.0",
      "io.circe" %% "circe-fs2"            % "0.13.0"
    )
  )

  val upperbound = Seq(libraryDependencies += "org.systemfw" %% "upperbound" % "0.2.0-M1")

  val Cormorant = Seq(
    libraryDependencies ++= Seq(
      "io.chrisdavenport" %% "cormorant-core"    % cormorantVersion,
      "io.chrisdavenport" %% "cormorant-generic" % cormorantVersion
    )
  )

  val XML = Seq(
    libraryDependencies ++= Seq(
      "javax.xml.bind"   % "jaxb-api"   % "2.3.1",
      "com.sun.xml.bind" % "jaxb-core"  % "2.3.0.1",
      "com.sun.xml.bind" % "jaxb-impl"  % "2.3.3",
      "javax.activation" % "activation" % "1.1.1"
    )
  )

  val sshj = Seq(libraryDependencies += "com.hierynomus" % "sshj" % sshjVersion)

}
