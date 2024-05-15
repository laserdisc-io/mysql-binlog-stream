organization := "io.laserdisc"
name         := "mysql-binlog-stream"

lazy val scala213               = "2.13.14"
lazy val scala3                 = "3.3.3"
lazy val supportedScalaVersions = List(scala213, scala3)

ThisBuild / crossScalaVersions := supportedScalaVersions
ThisBuild / scalaVersion       := scala3

lazy val commonSettings = Seq(
  organization := "io.laserdisc",
  developers := List(
    Developer("semenodm", "Dmytro Semenov", "sdo.semenov@gmail.com", url("https://github.com/semenodm")),
    Developer("barryoneill", "Barry O'Neill", "", url("https://github.com/barryoneill"))
  ),
  licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT"))),
  homepage                 := Some(url("https://github.com/laserdisc-io/fs2-aws")),
  Test / parallelExecution := false,
  Test / fork              := true,
  Test / scalacOptions ++= BuildOptions.scalacTestOptions,
  scalacOptions ++= BuildOptions.scalacOptions(scalaVersion.value),
  libraryDependencies ++= BuildOptions.compilerPlugins(scalaVersion.value)
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
