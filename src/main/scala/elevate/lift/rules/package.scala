package elevate.lift

import elevate.core.strategies.predicate._
import elevate.lift.strategies.traversal._
import elevate.core.{Failure, Lift, RewriteResult, Strategy, Success}
import lift.core._

package object rules {

  case object betaReduction extends Strategy[Lift] {
    def apply(e: Lift): RewriteResult[Lift] = e match {
      case Apply(f, x) => lifting.liftFunExpr(f) match {
        case lifting.Reducing(lf) => Success(lf(x))
        case _ => Failure(betaReduction)
      }
      case _ => Failure(betaReduction)
    }
  }

  case object etaReduction extends Strategy[Lift]  {
    def apply(e: Lift): RewriteResult[Lift] = e match {
      case Lambda(x1, Apply(f, x2)) if x1 == x2 && !contains[Lift](x1).apply(f) => Success(f)
      case _ => Failure(etaReduction)
    }
  }

  case object etaAbstraction extends Strategy[Lift] {
    // TODO? check that `f` is a function (i.e. has a function type)
    def apply(e: Lift): RewriteResult[Lift] = e match {
      case f =>
        val x = Identifier(freshName("η"))
        Success(Lambda(x, Apply(f, x)))
    }
  }
}
