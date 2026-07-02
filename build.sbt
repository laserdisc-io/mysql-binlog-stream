import laserdisc.sbt.CompileTarget.Scala2And3LTS
import laserdisc.sbt.LaserDiscDevelopers._

ThisBuild / laserdiscRepoName      := "mysql-binlog-stream"
ThisBuild / laserdiscCompileTarget := Scala2And3LTS

lazy val commonSettings = Seq(
  Test / parallelExecution := false,
  Test / fork              := true,
  developers               := List(Dmytro, Barry)
)

lazy val noPublishSettings = Seq(
  publish         := {},
  publishLocal    := {},
  publishArtifact := false
)

lazy val dockerPublishSettings = Seq(
  Docker / maintainer         := "Dmytro Semenov <sdo.semenov@gmail.com>",
  Docker / dockerExposedPorts := Seq(),
  dockerBaseImage             := "openjdk:17",
  dockerUpdateLatest          := true,
  Universal / javaOptions ++= Seq(
    "-J-XX:InitialRAMPercentage=70",
    "-J-XX:MaxRAMPercentage=85"
  )
)

lazy val root = project
  .in(file("."))
  .settings(name := "mysql-binlog-stream")
  .settings(commonSettings)
  .settings(noPublishSettings)
  .aggregate(
    `mysql-binlog-stream-shared`,
    `binlog-stream-models`,
    `binlog-stream`,
    `mysql-binlog-stream-examples`
  )
  .enablePlugins(LaserDiscDefaultsPlugin)

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
    Dependencies.Circe
  )

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
