package woot

import util.Random
import org.specs2._
import org.specs2.scalacheck.Parameters
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.scalacheck.Arbitrary.arbitrary

class WootModelSpec extends Specification with ScalaCheck {

  // More tests, longer texts (nb: the run time of these tests is of the ordermaxSize ^ 3)
  implicit val params = Parameters(minTestsOk=250, maxSize=200)

  def is = s2"""
    WOOT Model
      local insert preserves original text           $preserve
      insert order is irrelevant                     $order
      concurrent inserting produces consistent text  $concurrent
  """

  lazy val preserve = prop { (text: String) =>
    val (wstring, _) = applyWoot(text)
    wstring.text must_== text
  }

  lazy val order = prop { (text: String, siteId: String, clockStart: Int) =>
    // Generate the operations on a local site:
    val (_, ops) = applyWoot(text)

    // Shuffle the operations and apply them on a new site:
    val empty = WString(SiteId(siteId), ClockValue(clockStart))
    val integrated = (Random shuffle ops).foldLeft(empty) { _ integrate _ }

    // The text on the new site will be the same as the original:
    integrated.text must_== text
  }

  lazy val concurrent = forAll(nonEmptyString,nonEmptyString,nonEmptyString) { (starting: String, text1: String, text2: String) =>
    val randomPos = Gen.choose(0, starting.length)
    forAll(randomPos, randomPos) { (insert1: Int, insert2: Int) =>
      // Two sites starting with the same text:
      val (site1, ops) = applyWoot(starting)
      val site2 = ops.foldLeft(emptyDoc)(_ integrate _)

      // Each site then inserts different text at a random point
      val (site1Updated, site1Ops) = applyWoot(site1, text1, insert1)
      val (site2Updated, site2Ops) = applyWoot(site2, text2, insert2)

      // The sites exchange updates:
      val site1Final = site2Ops.foldLeft(site1Updated)(_ integrate _)
      val site2Final = site1Ops.foldLeft(site2Updated)(_ integrate _)

      // Both sites should have the same text:
      site1Final.text must_== site2Final.text
    }
  }

  val nonEmptyString: Gen[String] = Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString)

  private[this] def emptyDoc = WString(SiteId(Random.nextString(8)), ClockValue(Random.nextInt(100)))

  // Turn text into a sequence of operations on a new WString
  private[this] def applyWoot(text: String): (WString, Vector[InsertOp]) = {

    // We're going to accumulate a list of operations on a WString:
    type State = (WString, Vector[InsertOp])

    // The starting point is a new WString and no operations:
    val zero: State = (emptyDoc, Vector.empty)

    // Take each character and locally insert it at the correct position:
    (text.zipWithIndex).foldLeft[State](zero) {
      case ( (wstring, ops), (ch, pos) ) =>
        val (op, updated) = wstring.insert(ch, pos)
        (updated, op +: ops)
    }
  }

  // A variation on applyWoot where we insert at the same position in an existing WString
  private[this] def applyWoot(w: WString, text: String, pos: Int): (WString, Vector[InsertOp]) = {
    text.foldLeft((w,Vector.empty[InsertOp])) {
      case ((wstring, ops), ch) =>
        val (op, updated) = wstring.insert(ch, pos)
        (updated, op +: ops)
    }
  }

}