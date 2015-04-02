import woot._

import java.net.InetSocketAddress
import java.util.concurrent.Executors

import org.http4s.blaze.channel.SocketConnection
import org.http4s.blaze.channel.nio1.NIO1SocketServerChannelFactory
import org.http4s.blaze.pipeline.LeafBuilder
import org.http4s.server.blaze.{WebSocketSupport, Http1ServerStage}

import org.http4s.StaticFile
import org.http4s.MediaType
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

class WootServer(host: String, port: Int) {
  private val logger = getLogger
  logger.info(s"Starting Http4s-blaze WootServer on '$host:$port'")

  // For serving files for the file system:


  val staticPages = HttpService {
    case req if req.pathInfo.endsWith(".html") | req.pathInfo.endsWith(".js") =>
      logger.info(s"Resource: ${req.pathInfo}")
      StaticFile.fromResource(req.pathInfo.toString, Some(req))
        .map(Task.now)
        .getOrElse(NotFound())
  }


  // Endpoints for handling Woot

  private val ops = topic[String]()

  val wootEndpoints: HttpService = HttpService {
    case GET -> Root / "hello" =>
      Ok("Hello, better world.")

    case GET -> Root / "edit" / name =>

      logger.info(s"Edit for $name")

      def frameToMsg(f: WebSocketFrame) = f match {
        case Text(msg, _) => s"$name says: $msg"
        case _            => s"$name sent bad message! Booo..."
      }

      ops.publishOne(s"New user '$name' joined chat").flatMap { _ =>
        val src = Process.emit(Text(s"Welcome to the chat, $name!")) ++ ops.subscribe.map(Text(_))
        val snk = ops.publish.map(_ compose frameToMsg).onComplete(Process.await(ops.publishOne(s"$name left the chat"))(_ => Process.halt))
        WS(src, snk)
      }
  }

  def run(): Unit = {

    val addr = new InetSocketAddress(host, port)

    val service: HttpService = staticPages orElse wootEndpoints

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
  //val w = new WString( SiteId("main"), ClockValue(0)  )
  //println(w)  val ip = envOrNone("IP").getOrElse("0.0.0.0")
  val ip = envOrNone("HOST") getOrElse("0.0.0.0")
  val port = envOrNone("PORT") orElse envOrNone("HTTP_PORT") map(_.toInt) getOrElse(8080)
  new WootServer(ip, port).run()
}