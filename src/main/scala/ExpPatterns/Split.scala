package ExpPatterns

import AccPatterns.SplitAcc
import Core._
import Core.OperationalSemantics._
import Core.PhraseType.->
import Rewriting.RewriteToImperative
import DSL._
import apart.arithmetic.ArithExpr
import opencl.generator.OpenCLAST.Expression

case class Split(n: ArithExpr, array: Phrase[ExpType]) extends ExpPattern {

  override def typeCheck(): ExpType = {
    import TypeChecker._
    TypeChecker(array) match {
      case ExpType(ArrayType(m, dt)) =>
        ExpType(ArrayType(m/n, ArrayType(n, dt)))
      case t => error(t.toString, "ArrayType")
    }
  }

  override def substitute[T <: PhraseType](phrase: Phrase[T], `for`: Phrase[T]): ExpPattern = {
    Split(n, OperationalSemantics.substitute(phrase, `for`, array))
  }

  override def eval(s: Store): Data = {
    OperationalSemantics.eval(s, array) match {
      case ArrayData(arrayE) =>

        def split[T](n: ArithExpr, vector: Vector[T]): Vector[Vector[T]] = {
          val builder = Vector.newBuilder[Vector[T]]
          var vec = vector
          for (i <- 0 until vector.length / n.eval) {
            val (head, tail) = vec splitAt n.eval
            vec = tail
            builder += head
          }
          builder.result()
        }

        ArrayData(split(n, arrayE).map(ArrayData))

      case _ => throw new Exception("This should not happen")
    }
  }

  override def toC = ???

  override def toOpenCL: Expression = ???

  override def prettyPrint: String = s"(split ${n.toString} ${PrettyPrinter(array)})"

  override def rewriteToImperativeAcc(A: Phrase[AccType]): Phrase[CommandType] = {
    RewriteToImperative.acc(array, SplitAcc(A))
  }

  override def rewriteToImperativeExp(C: Phrase[->[ExpType, CommandType]]): Phrase[CommandType] = {
    RewriteToImperative.exp(array, λ(array.t) { x =>
      C(Split(n, x))
    })
  }
}