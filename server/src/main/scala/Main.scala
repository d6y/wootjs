import woot._

import java.net.InetSocketAddress
import java.util.concurrent.Executors

import org.http4s.blaze.channel.SocketConnection
import org.http4s.blaze.channel.nio1.NIO1SocketServerChannelFactory
import org.http4s.blaze.pipeline.LeafBuilder
import org.http4s.server.blaze.{WebSocketSupport, Http1ServerStage}

import org.http4s.{Request,StaticFile,Response}
import org.http4s.dsl._
import org.http4s.server.HttpService
import org.http4s.websocket.WebsocketBits._
import org.http4s.server.websocket.WS

import org.http4s.util.UrlCodingUtils.urlDecode

import scalaz.concurrent.Task
import scalaz.stream.{Sink,Process}
import scalaz.stream.async.topic

import scalaz.syntax.either._
import scalaz.{\/,-\/,\/-}

import scala.util.Properties.envOrNone

import org.log4s.getLogger

class WootRoutes {
  private val logger = getLogger
  private implicit val scheduledEC = Executors.newScheduledThreadPool(1)

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

  val service: HttpService = HttpService {

    // Joining an editing session, whicch sets up a web socket connection to the `ops` topic.
    case GET -> Root / "edit" / name =>

      import upickle._

      //
      // Set up source for documents and operations to send to the client:
      //

      val clientSite = SiteId.random
      logger.info(s"Subscribing $clientSite to document $name")

      val encodeOp: Operation => Text =
        op => Text(write(op))

      val clientCopy = doc.copy(site=clientSite)
      val document = Text(write(clientCopy))
      val src = Process.emit(document) ++ ops.subscribe.map(encodeOp)

      //
      // Set up sink for operations sent from the client:
      //

      // We can fail in at least two ways:
      // we're sent invalid JSON; or we're not sent text.

      val parse: String => Throwable \/ Operation =
        json => \/.fromTryCatchNonFatal { read[Operation](json) }

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

      val snk: Sink[Task, WebSocketFrame] = ops.publish.map(safeConsume).onComplete(cleanup)

      WS(src, snk)
  }

  private[this] def cleanup() = {
    logger.info("Subscriber left")
    Process.halt
  }
}

class StaticRoutes {
    private val logger = getLogger

    implicit class ReqOps(req: Request) {
      def endsWith(exts: String*): Boolean = exts.exists(req.pathInfo endsWith _)
      def serve(path: String = req.pathInfo) = {
        logger.info(s"Resource: ${req.pathInfo} -> $path")
        StaticFile.fromResource(path, Some(req))
          .map(Task.now)
          .getOrElse(NotFound())
      }
    }

    val service = HttpService {
      case req if req.pathInfo == "/"          => req.serve("/index.html")
      case req if req.endsWith(".html", ".js") => req.serve()
    }
}

class WootServer(host: String, port: Int) {
  private val logger = getLogger
  logger.info(s"Starting Http4s-blaze WootServer on '$host:$port'")

  def run(): Unit = {

    val addr = new InetSocketAddress(host, port)
    val service: HttpService = new StaticRoutes().service orElse new WootRoutes().service
    val pool = Executors.newCachedThreadPool()

    def pipelineBuilder(conn: SocketConnection) = {
      val s = new Http1ServerStage(service, Some(conn), pool) with WebSocketSupport
      LeafBuilder(s)
    }

    new NIO1SocketServerChannelFactory(pipelineBuilder, workerThreads=4, bufferSize=16*1024)
      .bind(addr)
      .run()
  }
}

object Main extends App {
  val ip = envOrNone("HOST") getOrElse("0.0.0.0")
  val port = envOrNone("PORT").map(_.toInt) getOrElse(8080)
  new WootServer(ip, port).run()
}