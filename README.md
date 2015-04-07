# Woot with Scala.js

## Running the web server

  $ sbt "project server" run

Then open _http://127.0.0.1:8080/index.html_.

## Code coverage

    sbt> project wootLib
    sbt> coverage
    sbt> test

Then open _woot-model/target/scala-2.11/scoverage-report/index.html_
