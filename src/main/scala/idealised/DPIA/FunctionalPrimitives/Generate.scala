package idealised.DPIA.FunctionalPrimitives

import idealised.DPIA.Compilation.{TranslationContext, TranslationToImperative}
import idealised.DPIA.Phrases._
import idealised.DPIA.Semantics.OperationalSemantics
import idealised.DPIA.Types._
import idealised.DPIA.DSL._
import idealised.DPIA.ImperativePrimitives.GenerateCont
import idealised.DPIA._

import scala.xml.Elem

final case class Generate(n: Nat,
                          w: AccessType,
                          dt: DataType,
                          f : Phrase[ExpType -> ExpType])
  extends ExpPrimitive {

  override val t: ExpType =
    (n: Nat) -> (w: AccessType) -> (dt: DataType) ->
      (f :: t"exp[idx($n), $read] -> exp[$dt, $w]") ->
        exp"[$n.$dt, $w]"

  def prettyPrint: String =
    s"${this.getClass.getSimpleName} (${PrettyPhrasePrinter(f)})"

  override def xmlPrinter: Elem =
    <generate n={ToString(n)} access={ToString(w)} dt={ToString(dt)}>
      <f type={ToString(ExpType(IndexType(n), read) -> ExpType(dt, w))}>
       {Phrases.xmlPrinter(f)}
      </f>
    </generate>

  def visitAndRebuild(fun: VisitAndRebuild.Visitor): Phrase[ExpType] =
    Generate(fun(n), w, fun(dt), VisitAndRebuild(f, fun))

  def eval(s: OperationalSemantics.Store): OperationalSemantics.Data = ???

  def acceptorTranslation(A: Phrase[AccType])
                         (implicit context: TranslationContext): Phrase[CommandType] = {
//    import T
//    acc()
    ???
  }

  override def mapAcceptorTranslation(f: Phrase[ExpType -> ExpType], A: Phrase[AccType])
                                     (implicit context: TranslationContext): Phrase[CommandType] = ???

  def continuationTranslation(C: Phrase[ExpType -> CommandType])
                             (implicit context: TranslationContext): Phrase[CommandType] = {
    import TranslationToImperative._

    // note: would not be necessary if generate was defined as indices + map
    C(GenerateCont(n, dt,
      fun(exp"[idx($n), $read]")(i =>
        fun(exp"[$dt, $read]" -> (comm: CommandType))(cont =>
          con(f(i))(fun(exp"[$dt, $read]")(g => Apply(cont, g)))
        ))
    ))
  }
}
