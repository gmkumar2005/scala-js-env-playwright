ThisBuild / organization := "github.gmkumar2005"
ThisBuild / scalaVersion := "2.12.18"

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
    Test / parallelExecution := true,
  )
