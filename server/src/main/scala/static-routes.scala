import org.http4s.{Request,StaticFile,Response}
import org.http4s.dsl._
import org.http4s.server.HttpService

import scalaz.concurrent.Task

import org.log4s.getLogger

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
      case req if req.pathInfo == "/"                  => req.serve("/index.html")
      case req if req.endsWith(".html", ".js", ".map") => req.serve()
    }
}