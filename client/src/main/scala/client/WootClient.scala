package client

import scala.util.{Try,Failure,Success}
import scala.scalajs.js
import js.annotation.JSExport

import woot._
import upickle._

@JSExport
class WootClient(
    onLoad:    js.Function1[String, Unit],
    onReceive: js.Function3[String,Boolean,Int,Unit]) {

  // This is the client copy of the document
  var doc = WString.empty()

  def openDocument(w: WString): Unit = {
    println(s"Becoming document: $w")
    doc = w
    onLoad(doc.text)
  }

  // The web socket works in terms of text
  type Json = String

  //
  // Local operations, producing a WChar to send across the network
  //

  @JSExport
  def insert(s: String, pos: Int): Json = {
    val (op, wstring) = doc.insert(s.head, pos)
    doc = wstring
    write(op)
  }

  @JSExport
  def delete(pos: Int): Json = {
    val (op, wstring) = doc.delete(pos)
    doc = wstring
    write(op)
  }

  //
  // Ingesting a remote operation or document
  //

  private[this] def applyOperation(op: Operation): Unit = {

    if (op.from == doc.site) {
      // We receive back operations we sent.
      // Useful for confirming receipt, but no action required.
      println(s"Ignoring echo of our own operation")
    } else {
      println(s"Applying $op")
      val (ops, wstring) = doc.integrate(op)

      // Become the updated document:
      doc = wstring

      // Side effects:
      ops.foreach {
        case InsertOp(ch, _) => onReceive(ch.alpha.toString, true,  doc.visibleIndexOf(ch.id))
        case DeleteOp(ch, _) => onReceive(ch.alpha.toString, false, doc.visibleIndexOf(ch.id))
      }
    }
  }

  @JSExport
  def ingest(json: Json): Unit = {
    println(s"Ingesting: $json")

    // We can be sent a whole WString or an individual Operation.
    // We'll simply try to decode each type:
    val result =
      Try(read[Operation](json)).map(applyOperation) orElse
        Try(read[WString](json)).map(openDocument)

  }
}