import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations.*
import sbtrelease.ReleaseStateTransformations.{checkSnapshotDependencies, inquireVersions, runClean}

organization := "io.github.gmkumar2005"
organizationName := "io.github.gmkumar2005"
scalaVersion := "3.8.4"
versionScheme := Some("early-semver")
licenses := Seq(
  "APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")
)
homepage := Some(url("https://www.akkagrpc.com"))
organizationHomepage := Some(url("https://www.akkagrpc.com"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/gmkumar2005/scala-js-env-playwright"),
    "scm:git@github.com:gmkumar2005/scala-js-env-playwright.git"
  )
)
developers := List(
  Developer(
    id = "gmkumar2005",
    name = "Kiran Kumar",
    email = "info@akkagrpc.com",
    url = url("https://www.akkagrpc.com")
  )
)

lazy val root = (project in file(".")).settings(
  name := "scala-js-env-playwright",
  libraryDependencies ++= Seq(
    "com.microsoft.playwright" % "playwright" % "1.61.0",
    "org.scala-js" %% "scalajs-js-envs" % "1.6.0",
    "com.google.jimfs" % "jimfs" % "1.3.0",
    "com.outr" %% "scribe" % "3.15.2",
    "org.typelevel" %% "cats-effect" % "3.5.7",
    "org.scala-js" %% "scalajs-js-envs-test-kit" % "1.6.0" % Test,
    "com.novocode" % "junit-interface" % "0.11" % Test
  ),
  javacOptions += "-nowarn",
  javacOptions -= "-Werror",
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseStep(action = st => Command.process("publishSigned", st, _ => ())),
    setNextVersion,
    commitNextVersion
  ),
  publishMavenStyle := true,
  publishTo := {
    if (isSnapshot.value)
      Some("central-snapshots".at("https://central.sonatype.com/repository/maven-snapshots/"))
    else localStaging.value
  },
  Test / parallelExecution := true,
  Test / publishArtifact := false,
  usePgpKeyHex("F7E440260BAE93EB4AD2723D6613CA76E011F638")
)
