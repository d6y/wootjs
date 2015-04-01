import scala.scalajs.js

import woot._

object Main extends js.JSApp {
  def main() = {
    val w = new WString( SiteId("main"), ClockValue(0)  )
    println(w)
  }
}