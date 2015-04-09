package client

import scala.util.{Try,Failure,Success}
import scala.scalajs.js
import js.annotation.JSExport

import woot._
import upickle._

@JSExport
class WootClient(onReceive: js.Function3[String,Boolean,Int,Unit]) {

  // This is the client copy of the document
  var doc = WString.empty()

  // The web socket works in terms of text
  type Json = String

  //
  // Local operations, producing a WChar to send across the network
  //

  @JSExport
  def insert(s: String, pos: Int): Json = {
    val (op, wstring) = doc.insert(s.head, pos);
    doc = wstring;
    write(op)
  }

  @JSExport
  def delete(s: String, pos: Int): Json = {
    "{}"
  }

  //
  // Ingesting a remote operation or document
  //

  @JSExport
  def ingest(json: String): Unit = {
    println(s"Ingesting: $json")

    // We can be sent a whole WString or an individual Operation.
    // We'll simply try to decode each type:
    val in = Try(read[WString](json)) orElse Try(read[Operation](json))

    in match {
      case Success(w: WString) =>
        println(s"Becoming new document: $w")
        doc = w

      case Success(op: Operation) if op.from == doc.site =>
        // We receive back operations we sent.
        // Useful for confirming receipt, but no action required.
        println(s"Ignoring echo of our own operation")

      case Success(op: Operation) =>
        println(s"Decoded Operation $op")
        val (ops, wstring) = doc.integrate(op)

        // Become the updated document:
        doc = wstring

        // Side effects:
        ops.foreach { op =>
          onReceive(op.wchar.alpha.toString, op.wchar.isVisible, doc.visibleIndexOf(op.wchar.id))
        }

      case Failure(err) =>
        println(s"Unrecognized $json -> $err")
    }
  }

}