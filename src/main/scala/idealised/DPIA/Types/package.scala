package idealised.DPIA

import idealised.DPIA.Types.TypeChecker._
import idealised.DPIA.Phrases.{Phrase, ToString}


package object Types {
  implicit class ReverseInferenceHelper(pt: PhraseType) {
    def ::[T <: PhraseType](p: Phrase[T]): Unit = p checkType pt
    def `:`[T <: PhraseType](p: Phrase[T]): Unit = p checkType pt
  }

  implicit class CheckHelper(p1: PhraseType) {
    def |(p2: PhraseType): (PhraseType => Unit) = (p: PhraseType) => {
      if (!(p == p1 || p == p2)) {
        error(ToString(p), expected = ToString(p1) + " or " + ToString(p2))
      }
    }
  }

  implicit final class ArrowDataType(private val self: DataType) {
    @inline def -> [B](y: B): B = y
    def →[B](y: B): B = ->(y)
  }

  implicit final class ArrowNat(private val self: Nat) {
    @inline def -> [B](y: B): B = y
    def →[B](y: B): B = ->(y)
  }

  implicit final class ArrowUnit(private val self: Unit) {
    @inline def -> [B](y: B): B = y
    def →[B](y: B): B = ->(y)
  }
}