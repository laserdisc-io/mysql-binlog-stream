import sbt.Keys.scalaSource

organization := "io.laserdisc"
name         := "mysql-binlog-stream"

ThisBuild / scalaVersion := "2.13.11"

lazy val commonSettings = Seq(
  organization := "io.laserdisc",
  developers := List(
    Developer("semenodm", "Dmytro Semenov", "sdo.semenov@gmail.com", url("https://github.com/semenodm")),
    Developer("barryoneill", "Barry O'Neill", "", url("https://github.com/barryoneill"))
  ),
  licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  homepage                    := Some(url("https://github.com/laserdisc-io/fs2-aws")),
  Compile / scalaSource       := baseDirectory.value / "app",
  Compile / resourceDirectory := baseDirectory.value / "conf",
  Test / scalaSource          := baseDirectory.value / "test",
  Test / resourceDirectory    := baseDirectory.value / "test_resources",
  Test / parallelExecution    := false,
  Test / fork                 := true,
  scalacOptions ++= Seq(
    "-encoding",
    "UTF-8",                         // source files are in UTF-8
    "-deprecation",                  // warn about use of deprecated APIs
    "-unchecked",                    // warn about unchecked type parameters
    "-feature",                      // warn about misused language features
    "-language:higherKinds",         // allow higher kinded types without `import scala.language.higherKinds`
    "-language:implicitConversions", // allow use of implicit conversions
    "-language:postfixOps",          // enable postfix ops
    "-Xlint:_,-byname-implicit",     // enable handy linter warnings
    "-Xfatal-warnings",              // turn compiler warnings into errors
    "-Ywarn-macros:after"            // allows the compiler to resolve implicit imports being flagged as unused
  ),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
)

lazy val noPublishSettings = Seq(
  publish         := {},
  publishLocal    := {},
  publishArtifact := false
)

lazy val dockerPublishSettings = Seq(
  Docker / maintainer         := "Dmytro Semenov <sdo.semenov@gmail.com>",
  Docker / dockerExposedPorts := Seq(),
  dockerBaseImage             := "openjdk:11",
  dockerUpdateLatest          := true,
  Universal / javaOptions ++= Seq(
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

lazy val `mysql-binlog-stream-examples` =
  (project in file("mysql-binlog-stream-examples"))
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
addCommandAlias("format", ";scalafmtAll;scalafmtSbt")
addCommandAlias("checkFormat", ";scalafmtCheckAll;scalafmtSbtCheck")

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
