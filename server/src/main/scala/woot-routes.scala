import woot._

import scalaz.stream.{Exchange, Process, time}
import scalaz.stream.async.topic
import scalaz.syntax.either._
import scalaz.{\/,-\/,\/-}
import scalaz.concurrent.Task

import org.http4s.server.websocket.WS
import org.http4s.websocket.WebsocketBits._

import org.http4s.server.HttpService
import org.http4s.dsl._

import org.log4s.getLogger
import upickle._

class WootRoutes {
  private val logger = getLogger

  // Local view of the document, starting from blank.
  // In a real system, we'd load from a backing store based on the name or ID of the document
  var doc = new WString(SiteId("server"), ClockValue(0))

  val updateDoc: Operation => Operation = op => {
    val (_, updated) = doc.integrate(op)
    doc = updated
    op
  }

  // The queue of WOOT operations:
  private val ops = topic[Operation]()

  // Encoding a decoding Web Socket data into Operations:
  val encodeOp: Operation => Text =
    op => Text(write(op))

  val parse: String => Throwable \/ Operation =
    json => \/.fromTryCatchNonFatal { read[Operation](json) }

  // We can fail in at least two ways:
  // we're sent invalid JSON; or we're not sent text.
  val decodeFrame: WebSocketFrame => Throwable \/ Operation =
    _ match {
      case Text(json, _) => parse(json).map(updateDoc)
      case nonText       => new IllegalArgumentException(s"Cannot handle: $nonText").left
   }

  val errorHandler: Throwable => Task[Unit] =
    err => {
      logger.warn(s"Failed to consume message: $err")
      Task.fail(err)
    }

   def safeConsume(consume: Operation => Task[Unit]): WebSocketFrame => Task[Unit] =
     ws => decodeFrame(ws).fold(errorHandler, consume)

   val service: HttpService = HttpService {

    // Joining an editing session, whicch sets up a web socket connection to the `ops` topic.
    case GET -> Root / "edit" / name =>

      // Set up source for documents and operations to send to the client:
      val clientSite = SiteId.random
      logger.info(s"Subscribing $clientSite to document $name")

      val clientCopy = doc.copy(site=clientSite)
      val document = Text(write(clientCopy))
      val src = Process.emit(document) ++ ops.subscribe.map(encodeOp)

      // Set up sink for operations sent from the client:
      val snk = ops.publish.map(safeConsume).onComplete(cleanup)

      WS(Exchange(src, snk))
  }

  private[this] def cleanup() = {
    logger.info("Subscriber left")
    Process.halt
  }
}