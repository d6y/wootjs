package woot

// # Each character has an `Id`.
// An `Id` is usually made up of a `SiteId` and a `ClockValue`, but there are two special cases
// called `Beginning` and `Ending`. This gives something for the first and last characters to point to.

sealed trait Id {
  def <(that: Id): Boolean
}

object Beginning extends Id {
  def <(that: Id) = true
}

object Ending extends Id {
  def <(that: Id) = false
}

case class SiteId(value: String) extends AnyVal {
  def <(that: SiteId) = this.value < that.value
}

object SiteId {
  import util.Random
  // Some arbitrary session identifier for a site:
  def random: SiteId =
    SiteId(Random.alphanumeric.take(8).mkString)
}

case class ClockValue(value: Int) extends AnyVal {
  def incr: ClockValue = ClockValue(value + 1)
  def <(that: ClockValue) = this.value < that.value
}


case class CharId(ns: SiteId, ng: ClockValue) extends Id {
  // The `<` comparison is defined in _Definition 7_ (p. 8) of [RR5580]
  def <(that: Id) = that match {
    case CharId(site, clock) => (ns < site) || (ns == site && ng < clock)
    case Beginning           => false
    case Ending              => true
  }
}

// # Character
case class WChar(id: CharId, alpha: Char, prev: Id, next: Id, isVisible: Boolean = true)

// # Operations are inserts or deletes
sealed trait Operation {
  def wchar: WChar
  def from:  SiteId
}

case class InsertOp(override val wchar: WChar, override val from: SiteId) extends Operation
case class DeleteOp(override val wchar: WChar, override val from: SiteId) extends Operation

// Convenience for getting a empty document
object WString {
  def empty(site: SiteId = SiteId.random) =
    WString(site, ClockValue(0))
}

// # String representation
// Note there there is no `WChar` representation of Beginning and Ending: they are not included in the vector.
// The intention is for this data structure to be immutable: the `integrate` and `delete` operations produce new `WString` instances.
case class WString(
    site:     SiteId,
    nextTick: ClockValue,
    chars:    Vector[WChar]     = Vector.empty,
    queue:    Vector[Operation] = Vector.empty) {

  lazy val visible = chars.filter(_.isVisible)

  // ## The visible text
  lazy val text: String = visible.map(_.alpha).mkString

  // When a character is added locally, the clock increments
  private def tick: WString = copy(nextTick = nextTick.incr)

  // The largest clock value for a site, when incremented, is the starting clock value for that site.
  def maxClockValue(site: SiteId): ClockValue = ClockValue(
    chars.filter(_.id.ns == site).map(_.id.ng.value).foldLeft(0) {
      math.max
    })

  // ## Insert a `WChar` into the internal vector at position `pos`, returning a new `WString`
  // Position are indexed from zero
  //
  private[this] def ins(char: WChar, pos: Int): WString = {
    val (before, after) = chars splitAt pos
    copy(chars = (before :+ char) ++ after)
  }

  // ## Lookup the position in `chars` of a given `id`
  //
  // Note that the `id` is required to exist in the `WString`.
  private[this] def indexOf(id: Id): Int = {
    val p = id match {
      case Ending    => chars.length
      case Beginning => 0
      case _         => chars.indexWhere(_.id == id)
    }
    require(p != -1)
    p
  }

  // # Lookup the visible position of the given `id`.
  //
  // Useful for updating an external editor, for example.
  // Id must exist in the document.
  def visibleIndexOf(id: Id): Int = {

    // To allow a deleted character to be located in an editor
    // (e.g., to side effect and update the editor)
    // we will locate where a deleted character was:
    lazy val indexWas = chars.take(indexOf(id)).filter(_.isVisible).length

    val p = id match {
      case Ending    => visible.length
      case Beginning => 0
      case _         => indexWas
    }
    require(p != -1)
    p
  }

  // ## The parts of this `WString` between `prev` and `next`
  // ...but excluding the neighbours themselves as required (see [RR5580] p. 8)
  private[this] def subseq(prev: Id, next: Id): Vector[WChar] = {

    val from = prev match {
      case Beginning => 0
      case id        => indexOf(id) + 1
    }

    val until = next match {
      case Ending => chars.length
      case id     => indexOf(id)
    }

    chars.slice(from,until)
  }

  // # Applicability test
  private[this] def canIntegrate(op: Operation): Boolean = op match {
    case InsertOp(c,_) => canIntegrate(c.next) && canIntegrate(c.prev)
    case DeleteOp(c,_) => chars.exists(_.id == c.id)
  }

  private[this] def canIntegrate(id: Id): Boolean =
    id == Ending || id == Beginning || chars.exists(_.id == id)

  // # Waiting integrations
  // If we cannot currently integrate a character, it goes into a queue.
  private[this] def enqueue(op: Operation): WString = copy(queue = queue :+ op)

  @scala.annotation.tailrec
  private def dequeue(ops: Vector[Operation] = Vector.empty): (Vector[Operation], WString) = {

    def without(op: Operation): Vector[Operation] = queue.filterNot(_ == op)

    queue.find(canIntegrate) match {
      case None                => (ops, this)
      case Some(op @ InsertOp(c,_)) =>
        // Remove the operation from the queue, and integrate:
        val doc = copy(queue = without(op)).integrate(c, c.prev, c.next)
        doc.dequeue(op +: ops)
      case Some(op @ DeleteOp(c,_)) =>
        copy(queue = without(op)).hide(c).dequeue(op +: ops)
      }
  }

  // # Delete means making the character invisible.
  private def hide(c: WChar): WString = {
    val p = chars.indexWhere(_.id == c.id)
    require(p != -1)
    val replacement = c.copy(isVisible=false)
    val (before, rest) = chars splitAt p
    copy(chars = (before :+ replacement) ++ (rest drop 1) )
  }


  // # Integrate a remotely received insert or delete.
  // The result is a new `WString` plus a list of the operations that were applied.
  // The operations can be:
  // - empty, if the operation was queued
  // - one operation, the typical expected case
  // - more than one operation, if the integration unblocked items on the queue.
  def integrate(op: Operation): (Vector[Operation], WString) = op match {
    // - Don't insert the same ID twice:
    case InsertOp(c,_) if chars.exists(_.id == c.id) => (Vector.empty, this)

    // - Insert can go ahead if the next & prev exist:
    case InsertOp(c,_) if canIntegrate(op) =>
      val (ops, doc) = integrate(c, c.prev, c.next).dequeue()
      (op +: ops, doc)

    // - We can delete any char that exists:
    case DeleteOp(c,_) if canIntegrate(op) => (Vector(op), hide(c))

    // - Anything else goes onto the queue for another time:
    case _                                 => (Vector.empty, enqueue(op))
  }

  @scala.annotation.tailrec
  private def integrate(c: WChar, before: Id, after: Id): WString = {

    // Looking at all the characters between the previous and next positions:
    subseq(before, after) match {
      // - when where's no option about where to insert, perform the insert
      case Vector() =>
        ins(c, indexOf(after))

      // - when there's a choice, locate an insert point based on `Id.<`
      case search: Vector[WChar] =>
        val L: Vector[Id] = before +: trim(search).map(_.id) :+ after
        val i = math.max(1, math.min(L.length-1, L.takeWhile( _ < c.id ).length))
        integrate(c, L(i-1), L(i))
    }
  }

  // Don't consider characters that have a `prev` or `next` in the set of
  // locations to consider (i.e., ones that are between the insert points of interest)
  // See last paragraph of first column of page 5 of CSCW06 (relating to figure 4b)
  private[this] def trim(cs: Vector[WChar]): Vector[WChar] =
    for {
      c <- cs
      if cs.forall(x => x.id != c.next && x.id != c.prev)
    } yield c

  // # A local insert of a character, producing an operation to send around the network.
  def insert(ch: Char, visiblePos: Int): (InsertOp, WString) = {

    require(visiblePos > -1)

    val prev: Id =
      if (visiblePos == 0) Beginning
      else visible(visiblePos-1).id

    val next: Id =
      if (visiblePos >= visible.length) Ending
      else visible(visiblePos).id // Not +1 because we are inserting just before this char

    val wchar = WChar(CharId(site, nextTick), ch, prev, next)
    val op = InsertOp(wchar, site)
    val (_, wstring) = integrate(op)

    (op, wstring.tick)
  }

  // # Remove a local character, giving the operation and updated `WString`.
  def delete(visiblePos: Int): (DeleteOp, WString) = {

    require(visiblePos > -1)
    require(visiblePos < visible.length)

    val wchar = visible(visiblePos)
    val op = DeleteOp(wchar, site)
    val (_, wstring) = integrate(op)

    (op, wstring)
  }
}