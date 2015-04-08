import woot._

import java.net.InetSocketAddress
import java.util.concurrent.Executors

import org.http4s.blaze.channel.SocketConnection
import org.http4s.blaze.channel.nio1.NIO1SocketServerChannelFactory
import org.http4s.blaze.pipeline.LeafBuilder
import org.http4s.server.blaze.{WebSocketSupport, Http1ServerStage}

import org.http4s.{StaticFile,MediaType,Response}
import org.http4s.dsl._
import org.http4s.server.HttpService
import org.http4s.websocket.WebsocketBits._
import org.http4s.server.websocket.WS

import org.http4s.util.UrlCodingUtils.urlDecode

import scalaz.concurrent.Task
import scalaz.stream.Process
import scalaz.stream.async.topic

import scala.util.Properties.envOrNone

import org.log4s.getLogger

import util.Random

class WootRoutes {
  private val logger = getLogger
  private implicit val scheduledEC = Executors.newScheduledThreadPool(1)

  // Local view of the document, starting from blank.
  // In a real system, we'd load from a backing store based on the name or ID of the document
  val doc = new WString(SiteId("server"), ClockValue(0))

  // The queue of WOOT operations:
  private val ops = topic[Operation]()

  val service: HttpService = HttpService {

    // Joining an editing session, whicch sets up a web socket connection to the `ops` topic.
    case GET -> Root / "edit" / name =>

      import upickle._

      // TODO: wsf => Throwable \/ Operation
      val decodeFrame: WebSocketFrame => Operation =
        _ match {
          case Text(json, _) =>
            logger.info(s"Received: $json")
            read[Operation](json)
//          case nonText       =>
//            logger.warn(s"Non Text received: $nonText")
//            NoOp
       }



      val encodeOp: Operation => Text = { op =>
        val json = write(op)
        logger.debug(s"encodeOp: $json")
        Text(json)
      }

      val clientSite: SiteId = randomSite
      logger.info(s"Subscribing $clientSite to document $name")

      val clientCopy = doc.copy(site=clientSite)
      val document = Text(write(clientCopy))

      val src = Process.emit(document) ++ ops.subscribe.map(encodeOp)
      val snk = ops.publish.map(_ compose decodeFrame)
      WS(src, snk)

      // TODO: shutdown considerations
      //val snk = ops.publish.map(_ compose decodeFrame).onComplete(Process.await(ops.publishOne(s"$name left the chat"))(_ => Process.halt))
  }

  // Some arbitrary session identifier for the site:
  private[this] def randomSite: SiteId =
    SiteId(Random.alphanumeric.take(8).mkString)

}

class StaticRoutes {
    private val logger = getLogger

    val service = HttpService {
      case req if req.pathInfo.endsWith(".html") | req.pathInfo.endsWith(".js") =>
        logger.info(s"Resource: ${req.pathInfo}")
        StaticFile.fromResource(req.pathInfo.toString, Some(req))
          .map(Task.now)
          .getOrElse(NotFound())
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