import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations.*
import xerial.sbt.Sonatype.*
organization := "io.github.gmkumar2005"
organizationName := "io.github.gmkumar2005"
scalaVersion := "2.12.18"
sonatypeProfileName := "io.github.gmkumar2005"
versionScheme := Some("early-semver")
licenses := Seq(
  "APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")
)
sonatypeProjectHosting := Some(
  GitHubHosting("gmkumar2005", "scala-js-env-playwright", "info@akkagrpc.com")
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
      "org.typelevel" %% "cats-effect" % "3.5.2",
      "org.scala-js" %% "scalajs-js-envs-test-kit" % "1.4.0" % Test,
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
      ReleaseStep(action = Command.process("publishSigned", _)),
      setNextVersion,
      commitNextVersion
    ),
    publishTo := {
      val nexus = "https://s01.oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    // For all Sonatype accounts created on or after February 2021
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    Test / parallelExecution := true,
    Test / publishArtifact := false,
    usePgpKeyHex("F7E440260BAE93EB4AD2723D6613CA76E011F638")
  )
