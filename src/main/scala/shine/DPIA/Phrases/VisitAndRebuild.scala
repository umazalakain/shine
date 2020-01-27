package shine.DPIA.Phrases

import shine.DPIA.Types._
import shine.DPIA._

object VisitAndRebuild {

  class Visitor {
    def phrase[T <: PhraseType](p: Phrase[T]): Result[Phrase[T]] = Continue(p, this)

    def nat[N <: Nat](n: N): N = n

    def data[T <: DataType](dt: T): T = dt

    def natToNat[N <: NatToNat](ft: N): N = (ft match {
      case NatToNatLambda(n, body) => NatToNatLambda(nat(n), nat(body))
      case i: NatToNatIdentifier => i
    }).asInstanceOf[N]

    def natToData[N <: NatToData](ft: N): N = (ft match {
      case NatToDataLambda(n, body) => NatToDataLambda(nat(n), data(body))
      case i: NatToDataIdentifier => i
    }).asInstanceOf[N]

    def access(w: AccessType): AccessType = w

    def addressSpace(a: AddressSpace): AddressSpace = a

    abstract class Result[+T]
    case class Stop[T <: PhraseType](p: Phrase[T]) extends Result[Phrase[T]]
    case class Continue[T <: PhraseType](p: Phrase[T], v: Visitor) extends Result[Phrase[T]]
  }

  def apply[T <: PhraseType](phrase: Phrase[T], v: Visitor): Phrase[T] = {
    v.phrase(phrase) match {
      case r: v.Stop[T]@unchecked => r.p
      case c: v.Continue[T]@unchecked =>
        val v = c.v
        (c.p match {
          case i: Identifier[T] =>
            Identifier(i.name, visitPhraseTypeAndRebuild(i.t, v))

          case Lambda(x, p) =>
            apply(x, v) match {
              case newX: Identifier[_] => Lambda(newX, apply(p, v))
              case badX => throw new Exception(s"${badX} is not an identifier")
            }

          case Apply(p, q) =>
            Apply(apply(p, v), apply(q, v))

          case dl @ DepLambda(a, p) =>
            DepLambda(a, apply(p, v))(dl.kn)

          case DepApply(p, a) => a match {
            case n: Nat =>
              DepApply[NatKind, T](apply(p, v).asInstanceOf[Phrase[NatKind `()->:` T]], v.nat(n))
            case dt: DataType =>
              DepApply[DataKind, T](apply(p, v).asInstanceOf[Phrase[DataKind `()->:` T]], v.data(dt))
            case ad: AddressSpace =>
              DepApply[AddressSpaceKind, T](apply(p, v).asInstanceOf[Phrase[AddressSpaceKind `()->:` T]], v.addressSpace(ad))
            case ac: AccessType =>
              DepApply[AccessKind, T](apply(p, v).asInstanceOf[Phrase[AccessKind `()->:` T]], v.access(ac))
            case n2n: NatToNat =>
              DepApply[NatToNatKind, T](apply(p, v).asInstanceOf[Phrase[NatToNatKind `()->:` T]], v.natToNat(n2n))
            case n2d: NatToData =>
              DepApply[NatToDataKind, T](apply(p, v).asInstanceOf[Phrase[NatToDataKind `()->:` T]], v.natToData(n2d))
            case ph: PhraseType => ???
          }

          case LetNat(binder, defn, body) => LetNat(binder, apply(defn, v), apply(body, v))

          case PhrasePair(p, q) => PhrasePair(apply(p, v), apply(q, v))

          case Proj1(p) => Proj1(apply(p, v))

          case Proj2(p) => Proj2(apply(p, v))

          case IfThenElse(cond, thenP, elseP) =>
            IfThenElse(apply(cond, v), apply(thenP, v), apply(elseP, v))

          case Literal(d) => Literal(d)

          case Natural(n) => Natural(n)

          case UnaryOp(op, x) => UnaryOp(op, apply(x, v))

          case BinOp(op, lhs, rhs) => BinOp(op, apply(lhs, v), apply(rhs, v))

          case c: Primitive[T] => c.visitAndRebuild(v)
        }).asInstanceOf[Phrase[T]]
    }
  }

  private def visitPhraseTypeAndRebuild(phraseType: PhraseType, v: Visitor): PhraseType = phraseType match {
    case ExpType(dt, w)                 => ExpType(v.data(dt), v.access(w))
    case AccType(dt)                => AccType(v.data(dt))
    case CommType()                 => CommType()
    case PhrasePairType(t1, t2)           => PhrasePairType(visitPhraseTypeAndRebuild(t1, v), visitPhraseTypeAndRebuild(t2, v))
    case FunType(inT, outT)         => FunType(visitPhraseTypeAndRebuild(inT, v), visitPhraseTypeAndRebuild(outT, v))
    case PassiveFunType(inT, outT)  => PassiveFunType(visitPhraseTypeAndRebuild(inT, v), visitPhraseTypeAndRebuild(outT, v))
    case dft @ DepFunType(x, t)           => DepFunType(x, visitPhraseTypeAndRebuild(t, v))(dft.kn)
  }

}