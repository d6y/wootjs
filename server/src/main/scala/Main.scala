import woot._

object Main extends App {
  val w = new WString( SiteId("main"), ClockValue(0)  )
  println(w)
}