name := "woot"

scalaVersion in ThisBuild := "2.11.6"

resolvers in ThisBuild += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

lazy val root = project.in(file(".")).aggregate(client, server).settings(
  publish := {},
  publishLocal := {}
)


lazy val wootModelSettings = Seq(
    name := "woot",
    version := "0.1-SNAPSHOT",
    unmanagedSourceDirectories in Compile +=
      baseDirectory.value / ".." / "woot-model" / "src" / "main" / "scala"
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
    name := "woot server",
    libraryDependencies   ++= scalacheck ++ specs2,
    scalacOptions in Test ++= Seq("-Yrangepos")
  )

lazy val scalacheck = Seq("org.scalacheck" %% "scalacheck" % "1.12.2" % "test")

lazy val specs2 = Seq(
  "org.specs2" %% "specs2-core"       % "3.2" % "test",
  "org.specs2" %% "specs2-scalacheck" % "3.2" % "test")