package rise.core

import rise.core.types._

object uniqueNames {
  private case class CountingVisitor(
    values: Map[Identifier, Int],
    others: Map[Kind.Identifier, Int]
  ) extends traversal.Visitor {
    override def visitExpr(e: Expr): traversal.Result[Expr] = e match {
      case Lambda(x, _) =>
        traversal.Continue(e, CountingVisitor(
          values + (x -> (values.getOrElse(x, 0) + 1)),
          others
        ))
      case DepLambda(x: Kind.Identifier, _) =>
        traversal.Continue(e, CountingVisitor(
          values,
          others + (x -> (others.getOrElse(x, 0) + 1))
        ))
      case _ => traversal.Continue(e, this)
    }

    override def visitType[T <: Type](t: T): traversal.Result[T] = {
      case class TypeVisitor(
        others: Map[Kind.Identifier, Int]
      ) extends traversal.Visitor {
        override def visitType[U <: Type](t: U): traversal.Result[U] = t match {
          case DepFunType(x: Kind.Identifier, _) =>
            traversal.Continue(t, TypeVisitor(
              others + (x -> (others.getOrElse(x, 0) + 1))
            ))
          case _ => traversal.Continue(t, this)
        }
      }
      traversal.types.DepthFirstGlobalResult(t, TypeVisitor(others))
      traversal.Continue(t, this)
    }
  }

  def check(e: Expr): Boolean = {
    traversal.DepthFirstGlobalResult(e, CountingVisitor(Map(), Map())) match {
      case traversal.Continue(_, CountingVisitor(values, others)) =>
        val valuesDup = values.filter({ case (_, n) => n > 1 })
        val othersDup = others.filter({ case (_, n) => n > 1 })
        valuesDup.isEmpty && othersDup.isEmpty
      case _ => throw new Exception("")
    }
  }

  def enforce(e: Expr): Expr = {
    var valN = -1
    def nextValN: Int = {
      valN += 1
      valN
    }

    var natN = -1
    def nextNatN: Int = {
      natN += 1
      natN
    }

    var dtN = -1
    def nextDtN: Int = {
      dtN += 1
      dtN
    }

    var aN = -1
    def nextAN: Int = {
      aN += 1
      aN
    }

    def renameInExpr(e: Expr)(values: Map[Identifier, Identifier],
                              others: Map[Kind.Identifier, Kind.Identifier]): Expr =
      traversal.DepthFirstLocalResult(e, RenamingVisitor(values, others))

    def renameInTypes[T <: Type](t: T)(others: Map[Kind.Identifier, Kind.Identifier]): T =
      traversal.types.DepthFirstLocalResult(t, TypeVisitor(others))

    def renameInNat(n: Nat)(others: Map[Kind.Identifier, Kind.Identifier]): Nat = {
      n.visitAndRebuild({
        case i: NatIdentifier =>
          others.get(i)
            .map(_.asInstanceOf[NatIdentifier])
            .getOrElse(i)
        case ae => ae
      })
    }

    case class RenamingVisitor(
      values: Map[Identifier, Identifier],
      others: Map[Kind.Identifier, Kind.Identifier]
    ) extends traversal.Visitor {
      override def visitExpr(e: Expr): traversal.Result[Expr] = e match {
        case x: Identifier =>
          traversal.Stop(values(x))

        case l@Lambda(x, b) =>
          val x2 = x.copy(s"x$nextValN")(renameInTypes(x.t)(others))
          val b2 = renameInExpr(b)(values + (x -> x2), others)
          val t2 = renameInTypes(l.t)(others)
          traversal.Stop(Lambda(x2, b2)(t2))

        case d@DepLambda(x: NatIdentifier, b) =>
          val x2 = NatIdentifier(s"n$nextNatN", x.range, x.isExplicit)
          val b2 = renameInExpr(b)(values, others + (x -> x2))
          val t2 = renameInTypes(d.t)(others + (x -> x2))
          traversal.Stop(DepLambda[NatKind](x2, b2)(t2))

        case d@DepLambda(x: DataTypeIdentifier, b) =>
          val x2 = DataTypeIdentifier(s"dt$nextDtN", x.isExplicit)
          val b2 = renameInExpr(b)(values, others + (x -> x2))
          val t2 = renameInTypes(d.t)(others)
          traversal.Stop(DepLambda[DataKind](x2, b2)(t2))

        case d@DepLambda(x: AddressSpaceIdentifier, b) =>
          val x2 = AddressSpaceIdentifier(s"a$nextAN", x.isExplicit)
          val b2 = renameInExpr(b)(values, others + (x -> x2))
          val t2 = renameInTypes(d.t)(others)
          traversal.Stop(DepLambda[AddressSpaceKind](x2, b2)(t2))

        case _ => traversal.Continue(e, this)
      }

      override def visitType[T <: Type](t: T): traversal.Result[T] =
        traversal.Stop(renameInTypes(t)(others))

      override def visitNat(ae: Nat): traversal.Result[Nat] =
        traversal.Stop(renameInNat(ae)(others))
    }

    case class TypeVisitor(
      others: Map[Kind.Identifier, Kind.Identifier]
    ) extends traversal.Visitor {
      override def visitType[U <: Type](t: U): traversal.Result[U] =
        t match {
          case i: DataTypeIdentifier =>
            traversal.Stop(others.get(i)
              .map(_.asInstanceOf[DataTypeIdentifier])
              .getOrElse(i).asInstanceOf[U])

          case DepFunType(x: NatIdentifier, b) =>
            val x2 = others.getOrElse(x,
              NatIdentifier(s"n$nextNatN", x.range, x.isExplicit)).asInstanceOf[NatIdentifier]
            val b2 = renameInTypes(b)(others + (x -> x2))
            traversal.Stop(DepFunType[NatKind, Type](x2, b2).asInstanceOf[U])

          case DepFunType(x: DataTypeIdentifier, b) =>
            val x2 = others.getOrElse(x,
              DataTypeIdentifier(s"dt$nextDtN", x.isExplicit)).asInstanceOf[DataTypeIdentifier]
            val b2 = renameInTypes(b)(others + (x -> x2))
            traversal.Stop(DepFunType[DataKind, Type](x2, b2).asInstanceOf[U])

          case DepFunType(x: AddressSpaceIdentifier, b) =>
            val x2 = others.getOrElse(x,
              AddressSpaceIdentifier(s"dt$nextAN", x.isExplicit)).asInstanceOf[AddressSpaceIdentifier]
            val b2 = renameInTypes(b)(others + (x -> x2))
            traversal.Stop(DepFunType[AddressSpaceKind, Type](x2, b2).asInstanceOf[U])

          case _ => traversal.Continue(t, this)
        }

      override def visitNat(ae: Nat): traversal.Result[Nat] =
        traversal.Stop(renameInNat(ae)(others))
    }

    val r = traversal.DepthFirstLocalResult(e, RenamingVisitor(Map(), Map()))
    r
  }
}
