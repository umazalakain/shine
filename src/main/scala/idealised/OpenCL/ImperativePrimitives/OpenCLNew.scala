package idealised.OpenCL.ImperativePrimitives

import idealised.DPIA.DSL.identifier
import idealised.DPIA.Phrases._
import idealised.DPIA.Semantics.OperationalSemantics
import idealised.DPIA.Semantics.OperationalSemantics._
import idealised.DPIA.Types._
import idealised.DPIA._

import scala.xml.Elem

final case class OpenCLNew(addrSpace: AddressSpace,
                           dt: DataType,
                           f: Phrase[VarType -> CommandType]) extends CommandPrimitive {

  override val t: CommandType =
    (addrSpace: AddressSpace) -> (dt: DataType) ->
      (f :: t"var[$dt] -> comm") -> comm

  override def eval(s: Store): Store = {
    val f_ = OperationalSemantics.eval(s, f)
    val arg = identifier(freshName("x"), f.t.inT)
    val newStore = OperationalSemantics.eval(s + (arg.name -> 0), f_(arg))
    newStore - arg.name
  }

  override def visitAndRebuild(fun: VisitAndRebuild.Visitor): Phrase[CommandType] = {
    OpenCLNew(addrSpace, fun(dt), VisitAndRebuild(f, fun))
  }

  override def prettyPrint: String = s"(new ${PrettyPhrasePrinter(f)})"

  override def xmlPrinter: Elem =
    <new addrSpace={ToString(addrSpace)} dt={ToString(dt)}>
      {Phrases.xmlPrinter(f)}
    </new>
}
