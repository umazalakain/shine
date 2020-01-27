package shine.DPIA.IntermediatePrimitives

import shine.DPIA.Compilation.TranslationContext
import shine.DPIA.DSL._
import shine.DPIA.Phrases._
import shine.DPIA.Types._
import shine.DPIA._

import scala.language.reflectiveCalls

object ReduceSeqI {
  def apply(n: Nat, dt1: DataType, dt2: DataType,
            f: Phrase[ExpType ->: ExpType ->: AccType ->: CommType],
            init: Phrase[ExpType],
            in: Phrase[ExpType],
            out: Phrase[ExpType ->: CommType],
            unroll: Boolean = false)
           (implicit context: TranslationContext): Phrase[CommType] =
  {
    `new`(dt2, acc =>
      //TODO implicit decisions happening here!
      (acc.wr :=|dt2| init) `;`
        comment("reduceSeq")`;`
        `for`(n, i => f(acc.rd)(in `@` i)(acc.wr), unroll) `;`
        out(acc.rd)
    )
  }
}