package rise.core

import rise.core.DSL._
import rise.core.TypeLevelDSL._
import rise.core.types._

class structuralEquality extends test_util.Tests {
  test("identity") {
    assert(fun(x => x) == fun(y => y))
  }

  test("reduce") {
    assert(
      nFun(n =>
        fun(ArrayType(n, int))(a => reduceSeq(fun(x => fun(y => x + y)))(0)(a))
      )
        ==
          nFun(m =>
            fun(ArrayType(m, int))(b =>
              reduceSeq(fun(y => fun(x => y + x)))(0)(b)
            )
          )
    )
  }

  test("reduce different init") {
    assert(
      nFun(n =>
        fun(ArrayType(n, int))(a => reduceSeq(fun(x => fun(y => x + y)))(0)(a))
      )
        !=
          nFun(m =>
            fun(ArrayType(m, int))(b =>
              reduceSeq(fun(y => fun(x => y + x)))(1)(b)
            )
          )
    )
  }

  test("reduce different function structure") {
    assert(
      nFun(n =>
        fun(ArrayType(n, int))(a => reduceSeq(fun(x => fun(y => x + y)))(0)(a))
      )
        !=
          nFun(m =>
            fun(ArrayType(m, int))(b =>
              reduceSeq(fun(y => fun(x => x + y)))(0)(b)
            )
          )
    )
  }

  test("reduce different type") {
    assert(
      nFun(n =>
        fun(ArrayType(n, int))(a => reduceSeq(fun(x => fun(y => x + y)))(0)(a))
      )
        !=
          nFun(m =>
            fun(ArrayType(m, f32))(b =>
              reduceSeq(fun(y => fun(x => y + x)))(0)(b)
            )
          )
    )
  }

  test("map different implementations") {
    assert(
      nFun(n => fun(ArrayType(n, int))(a => map(fun(x => x))(a)))
        !=
          nFun(m => fun(ArrayType(m, int))(b => mapSeq(fun(x => x))(b)))
    )
  }

  test("dependent function type using an array") {
    assert(
      expl((n: Nat) => expl((a: DataType) => expl((t: DataType) => ArrayType(n, a) ->: t)))
        ==
          expl((m: Nat) => expl((b: DataType) => expl((t: DataType) => ArrayType(m, b) ->: t)))
    )
  }
}