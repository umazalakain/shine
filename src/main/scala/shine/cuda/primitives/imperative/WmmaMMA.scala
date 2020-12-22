package shine.cuda.primitives.imperative

import shine.DPIA.Phrases._
import shine.DPIA.Semantics.OperationalSemantics.Store
import shine.DPIA.Types._
import shine.DPIA.{Nat, Phrases}
import shine.cuda.ast.Wmma

import scala.xml.Elem

case class WmmaMMA(m: Nat,
                   n: Nat,
                   k: Nat,
                   layoutA : MatrixLayout,
                   layoutB : MatrixLayout,
                   dataType: DataType,
                   dataTypeAcc: DataType,
                   aMatrix: Phrase[ExpType],
                   bMatrix: Phrase[ExpType],
                   cMatrix: Phrase[ExpType],
                   resultMatrix: Phrase[AccType]
                  ) extends CommandPrimitive {
  Wmma.checkDimensionsAndTypes(m, n, k, dataType, dataTypeAcc)

  aMatrix :: ExpType(WmmaAMatrix(m, n, k, dataType, layoutA), read)
  bMatrix :: ExpType(WmmaBMatrix(m, n, k, dataType, layoutB), read)
  cMatrix :: ExpType(WmmaAccumulator(m, n, k, dataTypeAcc), read)
  resultMatrix :: AccType(WmmaAccumulator(m, n, k, dataTypeAcc))

  override def eval(s: Store): Store = ???

  override def prettyPrint: String =
    s"WmmaMMA(${PrettyPhrasePrinter(aMatrix)}, ${PrettyPhrasePrinter(bMatrix)}," +
      s"${PrettyPhrasePrinter(cMatrix)}, ${PrettyPhrasePrinter(resultMatrix)})"

  override def xmlPrinter: Elem =
    <wmmaMMA m={ToString(m)} n={ToString(n)} k={ToString(k)} dt1={ToString(dataType)} dt2={ToString(dataTypeAcc)}>
      <fragment>
        {Phrases.xmlPrinter(aMatrix)}
      </fragment>
      <fragment>
        {Phrases.xmlPrinter(bMatrix)}
      </fragment>
      <fragment>
        {Phrases.xmlPrinter(cMatrix)}
      </fragment>
      <fragment>
        {Phrases.xmlPrinter(resultMatrix)}
      </fragment>
    </wmmaMMA>

  override def visitAndRebuild(fun: VisitAndRebuild.Visitor): Phrase[CommType] = {
    WmmaMMA(fun.nat(m), fun.nat(n), fun.nat(k), layoutA, layoutB,
      fun.data(dataType), fun.data(dataTypeAcc), VisitAndRebuild(aMatrix, fun), VisitAndRebuild(bMatrix, fun),
      VisitAndRebuild(cMatrix, fun), VisitAndRebuild(resultMatrix, fun))
  }
}
