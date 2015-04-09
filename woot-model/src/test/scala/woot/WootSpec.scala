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
    val (_, wstring) = applyWoot(text)
    wstring.text must_== text
  }

  lazy val order = prop { (text: String, siteId: String, clockStart: Int) =>
    // Generate the operations on a local site:
    val (ops, _) = applyWoot(text)

    // Shuffle the operations and apply them on a new site:
    val integrated = applyOps(Random shuffle ops)

    // The text on the new site will be the same as the original:
    integrated.text must_== text
  }

  lazy val concurrent = forAll(nonEmptyString,nonEmptyString,nonEmptyString) { (starting: String, text1: String, text2: String) =>
    val randomPos = Gen.choose(0, starting.length)
    forAll(randomPos, randomPos) { (insert1: Int, insert2: Int) =>
      // Two sites starting with the same text:
      val (ops, site1) = applyWoot(starting)
      val site2 = applyOps(ops)

      // Each site then inserts different text at a random point
      val (site1Ops, site1Updated) = applyWoot(site1, text1, insert1)
      val (site2Ops, site2Updated) = applyWoot(site2, text2, insert2)

      // The sites exchange updates:
      val site1Final = applyOps(site2Ops, site1Updated)
      val site2Final = applyOps(site1Ops, site2Updated)

      // Both sites should have the same text:
      site1Final.text must_== site2Final.text
    }
  }

  val nonEmptyString: Gen[String] = Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString)


  private[this] def applyOps(ops: Vector[Operation], startingDoc: WString = WString.empty()): WString = {
    ops.foldLeft(startingDoc) { (wstring, op) =>
      val (ops, doc) = wstring integrate op
      doc
    }
  }

  // Turn text into a sequence of operations on a new WString
  private[this] def applyWoot(text: String): (Vector[InsertOp], WString) = {

    // We're going to accumulate a list of operations on a WString:
    type State = (Vector[InsertOp], WString)

    // The starting point is a new WString and no operations:
    val zero: State = (Vector.empty, WString.empty())

    // Take each character and locally insert it at the correct position:
    (text.zipWithIndex).foldLeft[State](zero) {
      case ( (ops, wstring), (ch, pos) ) =>
        val (op, updated) = wstring.insert(ch, pos)
        (op +: ops, updated)
    }
  }

  // A variation on applyWoot where we insert at the same position in an existing WString
  private[this] def applyWoot(w: WString, text: String, pos: Int): (Vector[InsertOp], WString) = {
    text.foldLeft((Vector.empty[InsertOp], w)) {
      case ((ops, wstring), ch) =>
        val (op, updated) = wstring.insert(ch, pos)
        (op +: ops, updated)
    }
  }

}