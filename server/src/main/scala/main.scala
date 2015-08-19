
class WootServer(host: String, port: Int) {
  import java.net.InetSocketAddress
  import org.log4s.getLogger
  import org.http4s.server.blaze.BlazeBuilder

  private val logger = getLogger
  logger.info(s"Starting Http4s-blaze WootServer on '$host:$port'")

  def run(): Unit = {
    BlazeBuilder.
      bindSocketAddress(new InetSocketAddress(host, port)).
      withWebSockets(true).
      mountService(new StaticRoutes().service, "/").
      mountService(new WootRoutes().service, "/woot/").
      run.
      awaitShutdown()
    }
}

object Main extends App {
  import scala.util.Properties.envOrNone
  val ip   = envOrNone("HOST") getOrElse("0.0.0.0")
  val port = envOrNone("PORT").map(_.toInt) getOrElse(8080)
  new WootServer(ip, port).run()
}