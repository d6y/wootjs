# Woot with Scala.js

## For the Impatient

  $ sbt "project server" run

Then open _http://127.0.0.1:8080/index.html_.

## What Happens When You Run the Web Server

The above code will likely produce:

```
$ sbt "project server" run
[info] Loading global plugins from ...
[info] Loading project definition from wootjs/project/project
[info] Loading project definition from wootjs/project
[info] Set current project to woot
[info] Set current project to woot-server
[info] Fast optimizing wootjs/client/target/scala-2.11/woot-client-fastopt.js
[info] Running Main
2015-04-08 13:43:52 [run-main-0] INFO  WootServer - Starting Http4s-blaze WootServer on '0.0.0.0:8080'
...
```

Notice that the Scala.js compiler has run on the _woot-client_ project, to convert the client Scala code into JavaScript.  This JavaScript, _woot-client-fastopt.js_, is made available on the classpath of the server, so it can be included on the web page.  The web page is _server/src/main/resources/index.html_.

This reflects the structure of the project:

* _client_ - Scala source code, to be compiled to JavaScript.
* _server_ - Scala source code to run server-side, plus other assets to be served, such as HTML, CSS and plain old JavaScript.
* _woot-model_ - Scala source code shared by both the client and server projects. This is the WOOT algorithm, implemented once, used in the JVM and the JavaScript runtime.


## Code Coverage

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
