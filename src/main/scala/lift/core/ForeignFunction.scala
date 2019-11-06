package lift.core

import lift.core.types.Type

case class ForeignFunction(decl: ForeignFunction.Decl)(override val t: Type) extends Primitive {
  override def typeScheme: Type = t
  override def setType(t: Type): ForeignFunction = ForeignFunction(decl)(t)
  override def toString: String = decl.name
}

object ForeignFunction {
  case class Decl(name: String, definition: Option[Def])
  case class Def(params: Seq[String], body: String)
}
