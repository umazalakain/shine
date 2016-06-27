package AccPatterns

import Core._
import Core.OperationalSemantics._
import apart.arithmetic.ArithExpr
import opencl.generator.OpenCLAST.VarRef

import scala.xml.Elem

case class SndAcc(dt1: DataType,
                  dt2: DataType,
                  record: Phrase[AccType]) extends AccPattern {

  override def typeCheck(): AccType = {
    import TypeChecker._
    TypeChecker(record) match {
      case AccType(RecordType(fst, snd)) if fst == dt1 && snd == dt2 =>
        AccType(snd)
      case t => error(t.toString, "Something else")
    }
  }

  override def eval(s: Store): AccIdentifier = {
    OperationalSemantics.eval(s, record) match {
      case r: RecordIdentiers => r.snd
      case _ => throw new Exception("This should not happen")
    }
  }

  override def visitAndRebuild(fun: VisitAndRebuild.fun): Phrase[AccType] = {
    SndAcc(fun(dt1), fun(dt2), VisitAndRebuild(record, fun))
  }

  override def toOpenCL(ocl: ToOpenCL): VarRef = ???

  def toOpenCL(ocl: ToOpenCL, arrayAccess: List[(ArithExpr, ArithExpr)], tupleAccess: List[ArithExpr]): VarRef = ???

  override def prettyPrint: String = s"(SndAcc ${PrettyPrinter(record)})"

  override def xmlPrinter: Elem =
    <sndAcc dt1={dt1.toString} dt2={dt2.toString}>
      {Core.xmlPrinter(record)}
    </sndAcc>
}
