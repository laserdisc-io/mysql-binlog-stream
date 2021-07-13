import sbt.Keys.{ libraryDependencies, _ }
import sbt._

object Dependencies {

  val cirisVersion  = "1.2.1"
  val doobieVersion = "0.13.4"
  val circeVersion  = "0.14.1"
  val catsVersion   = "2.5.3"

  val AwsSdkV1Version = "1.12.43"
  val fs2AwsVersion   = "3.1.1"
  val ScanamoVersion  = "1.0.0-M15"

  val TestLib = Seq(
    libraryDependencies ++= Seq(
      "org.scalamock"     %% "scalamock"            % "5.1.0"  % Test,
      "org.scalatest"     %% "scalatest"            % "3.2.9"  % Test,
      "com.dimafeng"      %% "testcontainers-scala" % "0.39.5" % Test,
      "org.testcontainers" % "mysql"                % "1.16.0" % Test,
      "org.testcontainers" % "testcontainers"       % "1.16.0" % Test
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
      "ch.qos.logback" % "logback-classic" % "1.2.5",
      "ch.qos.logback" % "logback-core"    % "1.2.5",
      "org.slf4j"      % "jcl-over-slf4j"  % "1.7.32",
      "org.slf4j"      % "jul-to-slf4j"    % "1.7.32",
      "org.typelevel" %% "log4cats-slf4j"  % "1.3.1"
    )
  )

  val Persistence = Seq(
    libraryDependencies ++= Seq(
      "org.tpolecat"     %% "doobie-core"                 % doobieVersion,
      "org.tpolecat"     %% "doobie-hikari"               % doobieVersion, // HikariCP transactor.
      "org.tpolecat"     %% "doobie-refined"              % doobieVersion, // Postgres driver 42.1.4 + type mappings.
      "org.tpolecat"     %% "doobie-scalatest"            % doobieVersion % Test, // Support for doobie scalatest
      "mysql"             % "mysql-connector-java"        % "8.0.26",
      "com.github.shyiko" % "mysql-binlog-connector-java" % "0.21.0"
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
    libraryDependencies += "org.typelevel" %% "cats-effect" % catsVersion
  )

  val XML = Seq(
    libraryDependencies ++= Seq(
      "javax.xml.bind"   % "jaxb-api"   % "2.3.1",
      "com.sun.xml.bind" % "jaxb-impl"  % "3.0.1",
      "com.sun.xml.bind" % "jaxb-core"  % "3.0.1",
      "javax.activation" % "activation" % "1.1.1"
    )
  )

  val `fs2-aws` = Seq(
    libraryDependencies ++= Seq(
      "io.laserdisc" %% "fs2-aws"         % fs2AwsVersion,
      "io.laserdisc" %% "fs2-aws-testkit" % fs2AwsVersion % Test,
      /* Although fs2-aws is mostly based on the V2 of the AWS SDKs, the latest version of the
       * amazon-kinesis-producer  (which it also uses) is still stuck on the v1 SDKs:
       * https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer/pom.xml#L60
       * which means that if we want to be able to fetch OIDC creds from an EKS node, we need the v1 sts lib:
       * https://docs.aws.amazon.com/eks/latest/userguide/iam-roles-for-service-accounts-minimum-sdk.html
       */
      "com.amazonaws" % "aws-java-sdk-sts" % AwsSdkV1Version % "runtime"
    )
  )

  val Scanamo = Seq(
    libraryDependencies ++= Seq(
      "org.scanamo" %% "scanamo-cats-effect" % ScanamoVersion,
      "org.scanamo" %% "scanamo-testkit"     % ScanamoVersion % Test
    )
  )

}
