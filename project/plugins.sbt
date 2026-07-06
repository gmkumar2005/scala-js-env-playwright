val sbtTypelevelVersion = "0.7.5"
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.3.1")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.5.0")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.1")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.4.4")
// addSbtPlugin("org.typelevel" % "sbt-typelevel" % sbtTypelevelVersion)          // not published for sbt 2.x yet
// addSbtPlugin("org.typelevel" % "sbt-typelevel-scalafix" % sbtTypelevelVersion) // not published for sbt 2.x yet
// addSbtPlugin("org.typelevel" % "sbt-typelevel-site" % sbtTypelevelVersion)     // not published for sbt 2.x yet

addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "2.1.0")
