package client

import scala.util.Try
import scala.scalajs.js
import js.annotation.JSExport

import woot._
import upickle._

@JSExport
object WootClient {

  // This will be the client copy of the document
  var doc = WString.empty()

  @JSExport
  def insert(s: String, pos: Int): String = {
    val (op, wstring) = doc.insert(s.head, pos);
    doc = wstring;
    write(op)
  }

  @JSExport
  def delete(s: String, pos: Int): String = {
    "{}"
  }


  @JSExport
  def ingest(json: String): Unit = {
    println(s"Ingesting: $json")

    // We can be sent a whole WString (an object)
    // or an individual Operation (which is an array)
    // (These are the format used by uPickle)

    // We'll simply try to decode each type:
    val in = Try(read[WString](json)) orElse Try(read[Operation](json))

    in.toOption match {
      case Some(w: WString) =>
        println(s"Becoming new document: $w")
        doc = w

      case Some(op: Operation) if op.from == doc.site =>
        // We receive back operations we sent.
        // Useful for confirming receipt, but no action required.
        println(s"Ignoring message this site sent")

      case Some(op: Operation) =>
        println(s"Decoded Operation $op")
        val wstring = doc.integrate(op)
        println(s"Doc now $wstring")
        doc = wstring

      case err =>
        println(s"Unrecognized msg $err")
    }
  }

}