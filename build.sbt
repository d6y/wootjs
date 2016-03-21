lazy val root = project.in(file("."))
  .aggregate(client, server, modelJvm, modelJs)
  .dependsOn(client, server, modelJvm, modelJs)
  .settings(publish := {}, publishLocal := {})

lazy val commonSettings = Seq(
  version := "0.1.1",
  organization := "com.dallaway.richard",
  licenses += "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"),
  scalaVersion := "2.11.7",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-unchecked",
    "-feature",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-language:higherKinds",
    "-Xlint",
    "-Xfatal-warnings",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture"
  )
)

val upickleVersion = "0.2.8"

lazy val client = project.in(file("client"))
  .enablePlugins(ScalaJSPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "woot-client",
    testFrameworks += new TestFramework("scalacheck.ScalaCheckFramework"),
    javaOptions += "-Xmx2048m", // For tests, to avoid "OutOfMemoryError: Metaspace"
    libraryDependencies += "com.lihaoyi" %%% "upickle" % upickleVersion
  ) dependsOn(modelJs)

lazy val http4s = Seq(
  resolvers += {
    // http4s dependsOn scalaz-stream wich is available at
    "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
  },
  libraryDependencies ++= {
    val http4sVersion = "0.10.0"
    Seq(
      "org.http4s"     %% "http4s-blaze-server" % http4sVersion,
      "org.http4s"     %% "http4s-dsl"          % http4sVersion
    )
  }
)

lazy val server = project.in(file("server"))
  .settings(commonSettings: _*)
  .settings(Revolver.settings)
  .settings(http4s: _*)
  .settings(
    name := "woot-server",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "upickle" % upickleVersion,
      "ch.qos.logback"  % "logback-classic" % "1.1.3"
    ),
    resources in Compile ++= {
      def andSourceMap(aFile: java.io.File) = Seq(
        aFile,
        file(aFile.getAbsolutePath + ".map")
      )
      andSourceMap((fastOptJS in (client, Compile)).value.data)
    },
    testOptions in Test += Tests.Argument(TestFrameworks.ScalaCheck,
      "-maxSize",            "200", // Longer strings
      "-minSuccessfulTests", "250"  // More tests
    )
  ) dependsOn(modelJvm)

lazy val wootModel = crossProject
  .settings(commonSettings: _*)
  .settings(
    name := "woot-model",
    libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.12.5" % "test"
  )
  .jsSettings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.8.0"
  )

lazy val modelJvm = wootModel.jvm
lazy val modelJs = wootModel.js
