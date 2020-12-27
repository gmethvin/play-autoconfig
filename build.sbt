organization in ThisBuild := "io.methvin.play"
organizationName in ThisBuild := "Greg Methvin"
startYear in ThisBuild := Some(2018)
licenses in ThisBuild := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html"))
homepage in ThisBuild := Some(url("https://github.com/gmethvin/play-autoconfig"))
scmInfo in ThisBuild := Some(
  ScmInfo(url("https://github.com/gmethvin/play-autoconfig"), "scm:git@github.com:gmethvin/play-autoconfig.git")
)
developers in ThisBuild := List(
  Developer("gmethvin", "Greg Methvin", "greg@methvin.net", new URL("https://github.com/gmethvin"))
)

val PlayVersion = "2.8.0"
scalaVersion in ThisBuild := "2.13.3"
crossScalaVersions in ThisBuild := Seq("2.12.10", scalaVersion.value)

lazy val `autoconfig-macros` = (project in file("macros"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % PlayVersion,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scalatest" %% "scalatest" % "3.2.2" % Test
    )
  )

lazy val root = (project in file("."))
  .settings(
    PgpKeys.publishSigned := {},
    publish := {},
    publishLocal := {},
    publishArtifact := false,
    skip in publish := true
  )
  .aggregate(`autoconfig-macros`)

scalafmtOnCompile in ThisBuild := true

publishMavenStyle in ThisBuild := true
publishTo in ThisBuild := sonatypePublishToBundle.value

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

releaseCrossBuild := true
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
