package shine.OpenCL.FunctionalPrimitives

import shine.DPIA.Compilation.{TranslationContext, TranslationToImperative}
import shine.DPIA.DSL._
import shine.DPIA.Phrases._
import shine.DPIA.Semantics.OperationalSemantics._
import shine.DPIA.Types._
import shine.DPIA.Types.DataType._
import shine.DPIA._

import scala.xml.Elem

// set : (n : nat) -> (t : data) -> exp[n.t, read] -> exp[idx[n], read] -> exp[t, read] -> exp[n.t, read]

final case class Set(n: Nat,
                     dt: DataType,
                     array: Phrase[ExpType],
                     index: Phrase[ExpType],
                     value: Phrase[ExpType])
  extends ExpPrimitive {

  array :: expT(n`.`dt, read)
  index :: expT(idx(n), read)
  value :: expT(dt, read)
  override val t: ExpType = expT(n`.`dt, read)

  override def eval(s: Store): Data = ???

  override def visitAndRebuild(
                                fun: VisitAndRebuild.Visitor
                              ): Phrase[ExpType] = {
    Set(fun.nat(n),
        fun.data(dt),
        VisitAndRebuild(array, fun),
        VisitAndRebuild(index, fun),
        VisitAndRebuild(value, fun)
    )
  }

  override def prettyPrint: String =
    s"(${PrettyPhrasePrinter(array)})[${PrettyPhrasePrinter(index)}] = ${PrettyPhrasePrinter(value)}"

  override def xmlPrinter: Elem = ???

  override def acceptorTranslation(A: Phrase[AccType])(
    implicit context: TranslationContext
  ): Phrase[CommType] = ???

  override def continuationTranslation(C: Phrase[ExpType ->: CommType])(
    implicit context: TranslationContext
  ): Phrase[CommType] = {
    import TranslationToImperative._
    con(array)(λ(expT(n`.`dt, read))(e =>
      con(index)(fun(index.t)(i =>
        con(value)(λ(expT(dt, read))(x =>
        C(Set(n, dt, i, e, x))))))))
  }
}

object Set {
  def apply(array: Phrase[ExpType], index: Phrase[ExpType], value: Phrase[ExpType]): Set = {
    (array.t, index.t, value.t) match {
      case (ExpType(ArrayType(n1, dt1), _: read.type),
        ExpType(IndexType(n2), _: read.type),
        ExpType(dt2, _: read.type)
      ) if (n1 == n2) && (dt1 == dt2) =>
        Set(n1, dt1, array, index, value)
      case x => error(x.toString, "(exp[n.dt], exp[idx(n)], exp[dt])")
    }
  }
}
