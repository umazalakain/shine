package Core

import ir.ScalarType

sealed trait DataType

sealed abstract class BasicType extends DataType

object bool extends BasicType { override def toString = "bool" }

object int extends BasicType { override def toString = "int" }

object float extends BasicType { override def toString = "float" }

final case class IndexType(size: Nat) extends BasicType {
  override def toString = s"idx($size)"
}

final case class VectorType(size: Nat, elemType: BasicType) extends BasicType {
  override def toString = s"$elemType$size"
}

final case class ArrayType(size: Nat, elemType: DataType) extends DataType {
  override def toString = s"$size.$elemType"
}

final case class RecordType(fst: DataType, snd: DataType) extends DataType {
  override def toString = s"($fst x $snd)"
}

final case class DataTypeIdentifier(name: String) extends DataType {
  override def toString = name
}

object DataType {
  def toType(dt: DataType): ir.Type = {
    dt match {
      case b: BasicType => b match {
        case Core.bool => opencl.ir.Int
        case Core.int => opencl.ir.Int
        case Core.float => opencl.ir.Float
        case i: IndexType => opencl.ir.Int
        case v: VectorType => ir.VectorType(DataType.toType(v.elemType) match {
          case s: ScalarType => s
          case _ => throw new Exception("This should not happen")
        }, v.size)
      }
      case a: ArrayType => ir.ArrayType(DataType.toType(a.elemType), a.size)
      case r: RecordType => ir.TupleType(DataType.toType(r.fst), DataType.toType(r.snd))
      case _: DataTypeIdentifier => throw new Exception("This should not happen")
    }
  }

  def scalarType(dt: DataType): ir.ScalarType = {
    dt match {
      case b: BasicType => b match {
        case Core.bool => opencl.ir.Int
        case Core.int => opencl.ir.Int
        case Core.float => opencl.ir.Float
        case i: IndexType => opencl.ir.Int
        case v: VectorType => scalarType(v.elemType)
      }
      case a: ArrayType => scalarType(a.elemType)
      case r: RecordType => ???
      case _: DataTypeIdentifier => throw new Exception("This should not happen")
    }
  }

  def toString(dt: DataType): String = {
    dt match {
      case b: BasicType => b match {
        case Core.bool | Core.int => "int"
        case Core.float => "float"
        case i: IndexType => "int"
        case v: VectorType => toString(v.elemType) + v.size.toString
      }
      case _: RecordType => ???
      case _: ArrayType => ???
      case _: DataTypeIdentifier => throw new Exception("This should not happen")
    }
  }

  def getLengths(dt: DataType, tupleAccesss: List[Nat], list: List[Nat]): List[Nat] = {
    dt match {
      case _: BasicType => 1 :: list
      case r: RecordType =>
        val t = tupleAccesss.head
        val elemT = if (t == (1: Nat)) { r.fst } else if (t == (2: Nat)) { r.snd } else { throw new Exception("This should not happen") }
        getLengths(elemT, tupleAccesss.tail, list)
      case a: ArrayType => getLengths(a.elemType, tupleAccesss, a.size :: list)
      case _: DataTypeIdentifier => throw new Exception("This should not happen")
    }
  }

  implicit class RecordTypeConstructor(dt1: DataType) {
    def x(dt2: DataType) = RecordType(dt1, dt2)
  }

  implicit class ArrayTypeConstructor(s: Nat) {
    def `.`(dt: DataType) = ArrayType(s, dt)
  }

  def sizeInByte(dt: DataType): SizeInByte = sizeInByte(toType(dt))

  def sizeInByte(t: ir.Type): SizeInByte = {
    t match {
      case s: ir.ScalarType => SizeInByte(s.size)
      case v: ir.VectorType => sizeInByte(v.scalarT) * v.len
      case t: ir.TupleType => t.elemsT.map(sizeInByte).reduce(_+_)
      case a: ir.ArrayType => sizeInByte(a.elemT) * a.len
      case _: ir.NoType.type | _: ir.UndefType.type =>
        throw new Exception("This should not happen")
    }
  }
}