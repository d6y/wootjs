name := "woot"

scalaVersion in ThisBuild := "2.11.6"

resolvers in ThisBuild += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked", // Enable additional warnings where generated code depends on assumptions
  "-feature",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-Xlint", // Enable recommended additional warnings.
  "-Xfatal-warnings",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture"
  )

lazy val root = project.in(file(".")).aggregate(client, server).settings(
  publish := {},
  publishLocal := {}
)

lazy val wootModelSettings = Seq(
  name := "woot",
  version := "1.0.0-SNAPSHOT",
  unmanagedSourceDirectories in Compile +=
    baseDirectory.value / ".." / "woot-model" / "src" / "main" / "scala",
  unmanagedSourceDirectories in Test +=
    baseDirectory.value / ".." / "woot-model" / "src" / "test" / "scala",
  libraryDependencies ++= upickle.value ++ scalacheck.value
)

// This project exists to be able to generate code coverage reports
lazy val coverage = project.in(file("woot-model"))
  .settings(
  name := "coverage",
  libraryDependencies ++= scalacheck.value
)

lazy val client = project.in(file("client"))
  .enablePlugins(ScalaJSPlugin)
  .settings(wootModelSettings: _*)
  .settings(
    name := "woot-client",
    testFrameworks += new TestFramework("scalacheck.ScalaCheckFramework")
  )

lazy val server = project.in(file("server"))
  .settings(wootModelSettings: _*)
  .settings(
    name := "woot-server",
    libraryDependencies ++= http4s ++ logback,
    resources in Compile += (fastOptJS in (client, Compile)).value.data
  )


// I happen to like defining dependencies as Seq()
// https://github.com/vmunier/play-with-scalajs-example/issues/20#issuecomment-56589251

val upickle = Def.setting(Seq(
  "com.lihaoyi" %%% "upickle" % "0.2.8"
))

val scalacheck = Def.setting(Seq(
  "org.scalacheck" %%% "scalacheck" % "1.12.2" % "test"
))

val http4sVersion = "0.6.2"

lazy val http4s = Seq(
  "org.http4s" %% "http4s-blazeserver" % http4sVersion,
  "org.http4s" %% "http4s-dsl"         % http4sVersion
)

lazy val logback = Seq("ch.qos.logback" % "logback-classic" % "1.1.3")

// Clear the console when ~compile (for example) runs
triggeredMessage in ThisBuild := Watched.clearWhenTriggered
