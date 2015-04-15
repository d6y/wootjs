package woot

import util.Random

import org.scalacheck.{Properties, Gen}
import org.scalacheck.Prop.{forAll, BooleanOperators}
import org.scalacheck.Arbitrary, Arbitrary.arbitrary

object WootModelSpec extends Properties("WOOT Model") with WootOperationHelpers {

  import NonEmptyStringGenerator._
  import WootGenerator._

  property("local insert preserves original text") = forAll { (text: String) =>
    val (_, wstring) = WString.empty().insert(text)
    wstring.text == text
  }

  property("insert order is irrelevant") = forAll { (text: String, siteId: String, clockStart: Int) =>
    // Generate the operations on a local site:
    val (ops, _) = WString.empty().insert(text)

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
        val (ops, site1) = WString.empty().insert(starting)
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

  property("insert is idempotent") = forAll { text: NonEmptyString =>
    val (ops, doc) = WString.empty().insert(text)
    forAll(Gen.oneOf(ops)) { op: Operation =>
      val (ops, updated) = doc.integrate(op)
      (updated eq doc) && ops.length == 0
    }
  }

  property("local delete removes a character") = forAll { text: NonEmptyString =>
    forAll(text.chooseIndex) { index: Int =>
      val (_, doc)     = WString.empty().insert(text)
      val (_, updated) = doc.delete(index)
      val full: String = text // coerce to regular string to access take and drop
      val expected = full.take(index) + full.drop(index+1)
      (updated.text == expected) :| s"${updated.text} not $expected"
    }
  }

  property("remote delete produces consistent results") = forAll { text: NonEmptyString =>
    forAll(text.chooseIndex) { index: Int =>
      // Construct a document from the text and randomly delete a character:
      val (ops, doc)     = WString.empty().insert(text)
      val (op, updated)  = doc.delete(index)
      // Apply the operations to a new site (order is irrelevant)
      val site2 = applyOps(Random shuffle (ops :+ op))
      // The resultant text should be consistent:
      updated.text == site2.text
    }
  }

  property("delete operations should be emitted after corresponding insert") = {
    /*
    Imagine three sites, where there is a lag between site 1 and 3.
    Site 1 will insert a character, and site 2 will delete it.
    It takes 1 second for an insert from Site 1 to reach Site 2, but 5 seconds to reach site 3.
    Site 2 immediately deletes the character and broadcasts it.
    The delete will reach site 3 before the insert:

                     (1 sec)
      +--------+      INS A      +--------+
      | Site 1 |--------+------->| Site 2 |
      +--------+        |        +--------+
           ^            |             |
           |            |             |
           +------------+---- DEL A --+ (1 sec)
                        |             |
                        |             |
                        |             v
                        |        +--------+
                        +------->| Site 3 |
                                 +--------+
                       (5 secs)


    Site 3 will queue the delete until it receives the insert.
    When applying the insert, the delete will be dequeued and applied.

    To ensure the side-effects are applied in the correct order,
    the result of integrate at site 3 must be `Vector(insert,delete)`.

    Alternatively: we could optimize the implementation and turn a
    delete-before-an-insert into an automatic insert of an invisible
    character, with resultant operations of `Vector.empty`.
    */
    val wstring = WString.empty()

    forAll(wcharGen(wstring)) { wchar: WChar =>
      val insert = InsertOp(wchar, wstring.site)
      val delete = DeleteOp(wchar, wstring.site)

      val (_, doc) = wstring.integrate(delete)
      val (ops, _) = doc.integrate(insert)

      ops == Vector(insert, delete)
    }
  }


  property("site max clock value is the largest operation from that site") =
    forAll { text: NonEmptyString =>
      val originalDoc = WString.empty()
      val site = originalDoc.site
      val (ops, finalDoc) = originalDoc.insert(text)
      // As `nextTick` is an unused value, the maximum value will the
      // the starting point (less 1) plus the number of operations applied
      finalDoc.maxClockValue(site).value == (originalDoc.nextTick.value - 1 + ops.length)
    }

  property("can locate visible index of a character after deletion") = forAll { text: NonEmptyString =>
    forAll(text.chooseIndex) { index: Int =>
      // It is convenient to apply an operation to WOOT, such as delete, but
      // then be able to tell what the visible index was of the character
      // just removed.  Without this you need to lookup the character location
      // before you delete it (which is OK, but not always convenient)
      val (ops, doc)     = WString.empty().insert(text)
      val (op, updated)  = doc.delete(index)
      updated.visibleIndexOf(op.wchar.id) == index
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
  // A variation on WString.insert where we insert at the same position in an existing WString
  def applyWoot(w: WString, text: String, pos: Int): (Vector[Operation], WString) = {
    text.foldLeft((Vector.empty[Operation], w)) {
      case ((ops, wstring), ch) =>
        val (op, updated) = wstring.insert(ch, pos)
        (op +: ops, updated)
    }
  }
}

object WootGenerator {

 // Implementation design problem: doc.nextTick implies returning a modified doc
 def wcharGen(doc: WString): Gen[WChar] =
  for {
    alpha   <- arbitrary[Char]
    visible <- arbitrary[Boolean]
    id       = CharId(doc.site, doc.nextTick)
    pos     <- Gen.choose(0, doc.chars.length)
    prev     = if (pos == 0) Beginning else doc.chars(pos-1).id
    next     = if (pos >= doc.chars.length) Ending else doc.chars(pos).id
    } yield WChar(id, alpha, prev, next, visible)

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