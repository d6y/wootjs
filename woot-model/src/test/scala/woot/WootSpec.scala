package woot

import util.Random

import org.scalacheck.{Properties, Gen}
import org.scalacheck.Prop.{forAll, BooleanOperators}
import org.scalacheck.Arbitrary, Arbitrary.arbitrary

object WootModelSpec extends Properties("WOOT Model") with WootOperationHelpers {

  import NonEmptyStringGenerator._

  property("local insert preserves original text") = forAll { (text: String) =>
    val (_, wstring) = applyWoot(text)
    wstring.text == text
  }

  property("insert order is irrelevant") = forAll { (text: String, siteId: String, clockStart: Int) =>
    // Generate the operations on a local site:
    val (ops, _) = applyWoot(text)

    // Shuffle the operations and apply them on a new site:
    val integrated = applyOps(Random shuffle ops)

    // The text on the new site will be the same as the original:
    integrated.text == text
  }

  property("concurrent inserting produces consistent text") =
    forAll {
      (starting: NonEmptyString, text1: NonEmptyString, text2: NonEmptyString) =>
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
        site1Final.text == site2Final.text
    }
  }

  property("local delete removes a character") = forAll { text: NonEmptyString =>
    forAll(text.chooseIndex) { index: Int =>
      val (_, doc)     = applyWoot(text)
      val (_, updated) = doc.delete(index)
      val full: String = text // coerce to regular string to access take and drop
      val expected = full.take(index) + full.drop(index+1)
      (updated.text == expected) :| s"${updated.text} not $expected"
    }
  }


  property("remote delete produces consistent results") = forAll { text: NonEmptyString =>
    forAll(text.chooseIndex) { index: Int =>
      // Construct a document from the text and randomly delete a character:
      val (ops, doc)     = applyWoot(text)
      val (op, updated)  = doc.delete(index)
      // Apply the operations to a new site (order is irrelevant)
      val site2 = applyOps(Random shuffle (ops :+ op))
      // The resultant text should be consistent:
      updated.text == site2.text
    }
  }
}

// Functions to make it easier to work with WOOT and Operations in tests
trait WootOperationHelpers {
  // Apply a set of operations to a WString
  def applyOps(ops: Vector[Operation], startingDoc: WString = WString.empty()): WString = {
    ops.foldLeft(startingDoc) { (wstring, op) =>
      val (ops, doc) = wstring integrate op
      doc
    }
  }

  // Turn text into a sequence of operations on a new WString
  def applyWoot(text: String): (Vector[Operation], WString) = {

    // We're going to accumulate a list of operations on a WString:
    type State = (Vector[Operation], WString)

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
  def applyWoot(w: WString, text: String, pos: Int): (Vector[Operation], WString) = {
    text.foldLeft((Vector.empty[Operation], w)) {
      case ((ops, wstring), ch) =>
        val (op, updated) = wstring.insert(ch, pos)
        (op +: ops, updated)
    }
  }
}

object NonEmptyStringGenerator {

  case class NonEmptyString(text: String) extends AnyVal {
    def chooseIndex = Gen.choose(0, text.length-1)
  }

  // To allow us to treat the NonEmptyString as a String
  implicit def nes2s(nes: NonEmptyString): String = nes.text

  lazy val nonEmptyStringGen: Gen[NonEmptyString] =
    Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString).map(NonEmptyString.apply)

  implicit lazy val arbitraryNonEmptyString: Arbitrary[NonEmptyString] = Arbitrary(nonEmptyStringGen)
}