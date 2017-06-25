package idealised.OpenCL.CodeGeneration

import idealised.DPIA
import idealised.DPIA.Phrases._
import idealised.DPIA.Types.{AccType, CommandType, ExpType, IndexType, PairType, PhraseType}
import idealised.DPIA.DSL._
import idealised.DPIA.Semantics.OperationalSemantics.IndexData
import idealised.OpenCL.FunctionalPrimitives.OpenCLFunction
import ir.{ArrayType, ScalarType}
import lift.arithmetic.Cst
import opencl.generator.OpenCLAST.{ParamDecl, VarRef}
import opencl.ir.{GlobalMemory, LocalMemory}

import scala.collection.mutable

//
// Parameters (ParDecl) in global or local memory which have a non-array type in DPIA have to be
// represented as arrays of size 1 in OpenCL. Every reference to such a parameter is adjusted
// by indexing it with 0.
//
object AdaptKernelParameters {

  def apply(originalPhrase: Phrase[CommandType],
            params: Seq[ParamDecl],
            inputParams: Seq[Identifier[ExpType]]
           ): (Phrase[CommandType], Seq[ParamDecl]) = {
    val (newParams, scalarParamsInGlobalOrLocalMemory) = adaptParamDecls(params, inputParams)

    val rewrittenPhrase = VisitAndRebuild(originalPhrase, Visitor(scalarParamsInGlobalOrLocalMemory))

    (rewrittenPhrase, newParams)
  }

  private def adaptParamDecls(params: Seq[ParamDecl],
                      inputParams: Seq[Identifier[ExpType]]): (Seq[ParamDecl], Set[String]) = {
    val scalarParamsInGlobalOrLocalMemory = mutable.Set[String]()

    val newParams = params.map(paramDecl => {
      paramDecl.t match {
        case _: ScalarType =>
          paramDecl.addressSpace match {
            case GlobalMemory | LocalMemory =>
              // remember scalar parameters in global or local memory and change their type to an
              // array of size 1
              scalarParamsInGlobalOrLocalMemory.add(paramDecl.name)
              paramDecl.copy(t = ArrayType(paramDecl.t, 1))
            case _ => paramDecl
          }
        case _: ArrayType =>
          // make input parameters const
          if (inputParams.map(_.name).contains(paramDecl.name)) {
            paramDecl.copy(const = true)
          } else {
            paramDecl
          }
        case _ => paramDecl
      }
    })

    (newParams, scalarParamsInGlobalOrLocalMemory.toSet)
  }

  private case class Visitor(scalarParamsInGlobalOrLocalMemory: Set[String])
    extends VisitAndRebuild.Visitor
  {
    val zero = Literal(IndexData(0), IndexType(1))

    override def apply[T <: PhraseType](p: Phrase[T]): Result[Phrase[T]] = {
      p match {
        case p1: Proj1[T, _] => p1.pair match {
          case i: Identifier[PairType[T, _]] if scalarParamsInGlobalOrLocalMemory.contains(i.name) =>
            val j = i.`type` match {
              case PairType(_: ExpType, _: AccType) =>
                identifierAsSingletonArray(i.asInstanceOf[Identifier[PairType[ExpType, AccType]]])
            }
            Stop((Proj1(j) `@` zero).asInstanceOf[Phrase[T]])
          case _ => Continue(p, this)
        }

        case p2: Proj2[_, T] => p2.pair match {
          case i: Identifier[PairType[_, T]] if scalarParamsInGlobalOrLocalMemory.contains(i.name) =>
            val j = i.`type` match {
              case PairType(_: ExpType, _: AccType) =>
                identifierAsSingletonArray(i.asInstanceOf[Identifier[PairType[ExpType, AccType]]])
            }
            Stop((Proj2(j) `@` zero).asInstanceOf[Phrase[T]])
          case _ => Continue(p, this)
        }

        case i: Identifier[T] if scalarParamsInGlobalOrLocalMemory.contains(i.name) =>
          Stop((i.`type` match {
            case _: ExpType =>
              identifierAsSingletonArray(i.asInstanceOf[Identifier[ExpType]]) `@` zero
            case _: AccType =>
              identifierAsSingletonArray(i.asInstanceOf[Identifier[AccType]]) `@` zero
          }).asInstanceOf[Phrase[T]])

        case f: OpenCLFunction => println(PrettyPhrasePrinter(f))
          Continue(p, this)

        case _ => Continue(p, this)
      }
    }

    private def identifierAsSingletonArray[T <: PhraseType](i: Identifier[T]): Identifier[T] = {
      i.`type` match {
        case _: ExpType =>
          val ie = i.asInstanceOf[Identifier[ExpType]]
          ie.copy(`type` = ExpType(DPIA.Types.ArrayType(1, ie.`type`.dataType))).asInstanceOf[Identifier[T]]
        case _: AccType =>
          val ia = i.asInstanceOf[Identifier[AccType]]
          ia.copy(`type` = AccType(DPIA.Types.ArrayType(1, ia.`type`.dataType))).asInstanceOf[Identifier[T]]
        case PairType(_: ExpType, _: AccType) =>
          val ip = i.asInstanceOf[Identifier[PairType[ExpType, AccType]]]
          ip.copy(`type` = PairType(
            ExpType(DPIA.Types.ArrayType(1, ip.`type`.t1.dataType)),
              AccType(DPIA.Types.ArrayType(1, ip.`type`.t2.dataType)))).asInstanceOf[Identifier[T]]
      }
    }
  }


}
