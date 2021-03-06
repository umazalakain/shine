package shine.DPIA.ImperativePrimitives

import shine.DPIA.Phrases._
import shine.DPIA.Semantics.OperationalSemantics._
import shine.DPIA.Types._
import shine.DPIA.Types.DataType._
import shine.DPIA._

import scala.xml.Elem

final case class TransposeAcc(n: Nat, m: Nat, dt: DataType,
                              array: Phrase[AccType])
  extends AccPrimitive
{

  array :: accT(m`.`(n`.`dt))
  override val t: AccType = accT(n`.`(m`.`dt))

  override def visitAndRebuild(fun: VisitAndRebuild.Visitor): Phrase[AccType] = {
    TransposeAcc(fun.nat(n), fun.nat(m), fun.data(dt), VisitAndRebuild(array, fun))
  }

  override def eval(s: Store): AccIdentifier = ???

  override def prettyPrint: String =
    s"(transposeAcc ${PrettyPhrasePrinter(array)})"

  override def xmlPrinter: Elem =
    <transposeAcc n={ToString(n)} m={ToString(m)} dt={ToString(dt)}>
      {Phrases.xmlPrinter(array)}
    </transposeAcc>
}
