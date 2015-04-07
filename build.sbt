name := "woot"

scalaVersion in ThisBuild := "2.11.6"

resolvers in ThisBuild += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

lazy val root = project.in(file(".")).aggregate(client, server).settings(
  publish := {},
  publishLocal := {}
)

lazy val wootModelSettings = Seq(
    name := "woot",
    version := "1.0-SNAPSHOT",
    unmanagedSourceDirectories in Compile +=
      baseDirectory.value / ".." / "woot-model" / "src" / "main" / "scala",
    libraryDependencies += "be.doeraene" %%% "scalajs-pickling-core" % "0.4.0"
 )

lazy val wootLib = project.in(file("woot-model"))
  .settings(
    name := "woot-lib",
    libraryDependencies ++= scalacheck ++ specs2,
    scalacOptions in Test ++= Seq("-Yrangepos")
  )

lazy val client = project.in(file("client"))
  .enablePlugins(ScalaJSPlugin)
  .settings(wootModelSettings: _*)
  .settings(
    name := "woot-client"
    // Add JS-specific settings here
  )

lazy val server = project.in(file("server"))
  .settings(wootModelSettings: _*)
  .settings(
    name := "woot-server",
    libraryDependencies   ++= scalacheck ++ specs2 ++ http4s ++ logback,
    scalacOptions in Test ++= Seq("-Yrangepos")
  )

lazy val scalacheck = Seq("org.scalacheck" %% "scalacheck" % "1.12.2" % "test")

lazy val specs2 = Seq(
  "org.specs2" %% "specs2-core"       % "3.2" % "test",
  "org.specs2" %% "specs2-scalacheck" % "3.2" % "test")

val http4sVersion = "0.6.2"

lazy val http4s = Seq(
  "org.http4s" %% "http4s-blazeserver" % http4sVersion,
  "org.http4s" %% "http4s-dsl"         % http4sVersion
)

lazy val logback = Seq("ch.qos.logback" % "logback-classic" % "1.1.3")

// Clear the console when ~compile (for example) runs
triggeredMessage in ThisBuild := Watched.clearWhenTriggered
