import com.typesafe.sbt.packager.docker.ExecCmd
import sbt.Keys.scalaSource

organization := "io.laserdisc"
name         := "mysql-binlog-stream"

lazy val scala212               = "2.12.11"
lazy val scala213               = "2.13.2"
lazy val supportedScalaVersions = List(scala212, scala213)

scalaVersion       in ThisBuild := scala213
crossScalaVersions in ThisBuild := supportedScalaVersions

def commonOptions(scalaVersion: String) =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, 12)) =>
      Seq("-Ypartial-unification")
    case _ => Seq.empty
  }

lazy val commonSettings = Seq(
  organization := "io.laserdisc",
  developers := List(
    Developer(
      "semenodm",
      "Dmytro Semenov",
      "sdo.semenov@gmail.com",
      url("https://github.com/semenodm")
    )
  ),
  licenses                ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  homepage                 := Some(url("https://github.com/laserdisc-io/fs2-aws")),
  sources                  in (Compile, doc) := Seq(),
  scalaSource              in Compile        := baseDirectory.value / "app",
  scalaSource              in Test           := baseDirectory.value / "test",
  resourceDirectory        in Compile        := baseDirectory.value / "conf",
  resourceDirectory        in Test           := baseDirectory.value / "test_resources",
  Test / parallelExecution := false,
  fork                     in Test           := true,
  scalacOptions ++= commonOptions(scalaVersion.value) ++ Seq(
    "-encoding",
    "UTF-8",                         // source files are in UTF-8
    "-deprecation",                  // warn about use of deprecated APIs
    "-unchecked",                    // warn about unchecked type parameters
    "-feature",                      // warn about misused language features
    "-language:higherKinds",         // allow higher kinded types without `import scala.language.higherKinds`
    "-language:implicitConversions", // allow use of implicit conversions
    "-language:postfixOps",
    "-Xlint",             // enable handy linter warnings
    "-Xfatal-warnings",   // turn compiler warnings into errors
    "-Ywarn-macros:after" // allows the compiler to resolve implicit imports being flagged as unused
  ),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
)

lazy val noPublishSettings = Seq(
  publish         := {},
  publishLocal    := {},
  publishArtifact := false
)

lazy val dockerPublishSettings = Seq(
  maintainer         in Docker     := "Dmytro Semenov <sdo.semenov@gmail.com>",
  dockerBaseImage    := "openjdk:11",
  dockerExposedPorts in Docker     := Seq(),
  dockerUpdateLatest := true,
  javaOptions        in Universal ++= Seq(
    "-J-XX:InitialRAMPercentage=70",
    "-J-XX:MaxRAMPercentage=85"
  )
)

lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .settings(crossScalaVersions := Nil)
  .settings(noPublishSettings)
  .aggregate(
    `mysql-binlog-stream-shared`,
    `binlog-stream-models`,
    `binlog-stream`,
    `mysql-binlog-stream-examples`
  )

lazy val `mysql-binlog-stream-examples` = (project in file("mysql-binlog-stream-examples"))
  .settings(
    commonSettings,
    dockerPublishSettings,
    noPublishSettings,
    Dependencies.Config,
    Dependencies.XML
  )
  .dependsOn(`binlog-stream`)
  .enablePlugins(JavaAppPackaging)

lazy val `mysql-binlog-stream-shared` = (project in file("mysql-binlog-stream-shared"))
  .settings(
    commonSettings,
    Dependencies.TestLib,
    Dependencies.Circe,
    Dependencies.Cats
  )

addCommandAlias("build", ";checkFormat;clean;test;coverage")
addCommandAlias("format", ";scalafmt;test:scalafmt;scalafmtSbt")
addCommandAlias("checkFormat", ";scalafmtCheck;test:scalafmtCheck;scalafmtSbtCheck")

lazy val `binlog-stream` = (project in file("binlog-stream"))
  .settings(
    commonSettings,
    Dependencies.TestLib,
    Dependencies.Logging
  )
  .dependsOn(`mysql-binlog-stream-shared` % "compile->compile;test->test")
  .dependsOn(`binlog-stream-models`)

lazy val `binlog-stream-models` = (project in file("binlog-stream-models"))
  .settings(
    commonSettings,
    Dependencies.TestLib,
    Dependencies.Persistence
  )
  .dependsOn(`mysql-binlog-stream-shared` % "compile->compile;test->test")
