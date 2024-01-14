import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations.*
import xerial.sbt.Sonatype.*
ThisBuild / organization := "io.github.gmkumar2005"
ThisBuild / scalaVersion := "2.12.18"
ThisBuild / sonatypeProfileName := "com.github.gmkumar2005"
ThisBuild / licenses := Seq(
  "APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")
)
ThisBuild / sonatypeProjectHosting := Some(
  GitHubHosting("gmkumar2005", "scala-js-env-playwright", "info@akkagrpc.com")
)
ThisBuild / homepage := Some(url("https://www.akkagrpc.com"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/gmkumar2005/scala-js-env-playwright"),
    "scm:git@github.com:gmkumar2005/scala-js-env-playwright.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "gmkumar2005",
    name = "Kiran Kumar",
    email = "info@akkagrpc.com",
    url = url("https://www.akkagrpc.com")
  )
)

//publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
//publishTo := Some(Resolver.file("local-ivy-repo", file(Path.userHome.absolutePath + "/.ivy2/local"))(Patterns(true, Resolver.mavenStyleBasePattern)))
//val localIvyRepo = Resolver.file("local-ivy-repo", file(Path.userHome.absolutePath + "/.ivy2/local"))(Patterns(true, Resolver.mavenStyleBasePattern))

lazy val root = (project in file("."))
  .settings(
    name := "scala-js-env-playwright",
    libraryDependencies ++= Seq(
      "com.microsoft.playwright" % "playwright" % "1.40.0",
      "org.scala-js" %% "scalajs-js-envs" % "1.4.0",
      "com.google.jimfs" % "jimfs" % "1.2",
      "com.outr" %% "scribe" % "3.13.0",
      "org.scala-js" %% "scalajs-js-envs-test-kit" % "1.1.1" % Test,
      "com.novocode" % "junit-interface" % "0.11" % Test
    ),
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      setNextVersion,
      commitNextVersion
    ),
    publishTo := sonatypePublishToBundle.value,
    // For all Sonatype accounts created on or after February 2021
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    pomExtra := _pomExtra,
    Test / parallelExecution := false,
    Test / publishArtifact := false
  )

val _pomExtra =
  <url>https://github.com/gmkumar2005/scala-js-env-playwright</url>
  <licenses>
    <license>
      <name>Apache License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:gmkumar2005/scala-js-env-playwright.git</url>
    <connection>scm:git:git@github.com:gmkumar2005/scala-js-env-playwright.git</connection>
  </scm>
  <developers>
    <developer>
      <id>gmkumar2005</id>
      <name>Kiran Kumar</name>
      <url>https://github.com/gmkumar2005</url>
    </developer>
  </developers>
    <issueManagement>
      <system>GitHub</system>
      <url>https://github.com/gmkumar2005/scala-js-env-playwright/issues</url>
    </issueManagement>
