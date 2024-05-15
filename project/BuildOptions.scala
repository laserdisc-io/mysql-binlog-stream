import sbt.*

object BuildOptions {

  def scalacOptions(scalaVersion: String): Seq[String] =
    Scalac.Common ++ (CrossVersion.partialVersion(scalaVersion) match {
      case Some((3, _)) => Scalac.Version3x
      case Some((2, _)) => Scalac.Version2x
      case _            => Seq.empty
    })

  def scalacTestOptions: Seq[String] = Scalac.Test

  def compilerPlugins(scalaVersion: String): Seq[sbt.ModuleID] =
    Compiler.Common ++ (CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, _)) => Compiler.Scala2x
      case _            => Seq.empty
    })

  object Scalac {

    lazy val Common: Seq[String] = Seq(
      "-encoding",
      "UTF-8",
      "-deprecation",
      "-unchecked",
      "-feature",
      "-language:existentials,experimental.macros,higherKinds,implicitConversions,postfixOps",
      "-Wconf:src=src_managed/.*:silent",
      "-Xfatal-warnings"
    )

    lazy val Version3x: Seq[String] = Seq(
      "-Yretain-trees",
      "-Ykind-projector:underscores",
      "-source:future",
      "-language:adhocExtensions",
      "-Wconf:msg=`= _` has been deprecated; use `= uninitialized` instead.:s"
    )

    lazy val Test: Seq[String] = Seq(
      "-Wconf:msg=is not declared infix:s,msg=is declared 'open':s"
    )

    lazy val Version2x: Seq[String] = Seq(
      "-Xlint:-unused,_",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Ywarn-unused:implicits",
      "-Ywarn-unused:imports",
      "-Xsource:3",
      "-Xlint:_,-byname-implicit", // enable handy linter warnings except byname-implicit (see https://github.com/scala/bug/issues/12072)
      "-P:kind-projector:underscore-placeholders",
      "-Xlint",
      "-Ywarn-macros:after"
    )

  }

  object Compiler {

    lazy val Common: Seq[ModuleID] = Seq("org.scala-lang.modules" %% "scala-collection-compat" % "2.12.0")

    lazy val Scala2x: Seq[ModuleID] = Seq(
      compilerPlugin(("org.typelevel" %% "kind-projector" % "0.13.3").cross(CrossVersion.full)),
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
    )

  }

}
