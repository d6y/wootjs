package client

import scala.scalajs.js
import js.annotation.JSExport

import woot._
import upickle._

@JSExport
object WootClient {

  // This will be the client copy of the document
  var doc = WString.empty()

  @JSExport
  def ingest(json: String): Unit = {
    println(s"Ingesting: $json")
    println(s"Into: $doc")
    val in = read[WString](json)
    println(s"Decoded as: $in")
    // Become the document we have been sent:
    doc = in
  }
}