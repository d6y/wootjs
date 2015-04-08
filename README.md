# Woot with Scala.js

## Running the web server

  $ sbt "project server" run

Then open _http://127.0.0.1:8080/index.html_.

## Code coverage

    sbt> project wootLib
    sbt> coverage
    sbt> test

Then open _woot-model/target/scala-2.11/scoverage-report/index.html_

## Reference

* [Exporting Scala.js APIs to JavaScript](http://www.scala-js.org/doc/export-to-javascript.html)
* [µPickle 0.2.8](http://lihaoyi.github.io/upickle/) - the JVM and JavaScript JSON/case class serialization library used in this demo.
* [http4s](http://http4s.org/) - the server used in this demo.

## Scala.js Learning Path

If you're new to Scala:

* [Creative Scala](http://underscore.io/training/courses/creative-scala/) - a free course from Underscore teaching Scala using drawing primitives backed by Scala.js.

And then...

* [Scala-js.org Tutorial](http://www.scala-js.org/doc/tutorial.html)
* [Hands-on Scala.js](http://lihaoyi.github.io/hands-on-scala-js/#Hands-onScala.js)
