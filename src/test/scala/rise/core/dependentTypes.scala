package rise.core

import rise.core.TypedDSL._
import rise.core.TypeLevelDSL._
import rise.core.types._

class dependentTypes extends test_util.Tests {
  test("Infer int addition type") {
    val e =
      nFun(n =>
        fun(
          DepArrayType(n, n2dtFun(i => (i + 1) `.` f32)) ->: DepArrayType(
            n,
            n2dtFun(i => (i + 1) `.` f32)
          )
        )(xs => xs |> depMapSeq(nFun(_ => mapSeq(fun(x => x)))))
      )
    val inferred: Expr = TDSL.inferDependent(e)
    println(inferred)
    println(inferred.t)
  }

  test("Dependent pair construct") {
    val e = nFun(n =>
      fun(n `.` f32)(xs => dpair(n)(mapSeq(fun(x => x))(xs)))
    )
    val inferred: Expr = TDSL.inferDependent(e)
    println(inferred)
    print(inferred.t)
    util.gen.CProgram(inferred, "Foo_foo")
  }

  test("GEN: Dependent pair match") {
    val e = fun(n2dPairT(n => n`.`f32))(pair =>
      dmatch(pair)(nFun(n => fun(xs =>
        dpair(n)(mapSeq(fun(x => x))(xs) ::(n`.`f32))
      ))))
    val inferred: Expr = TDSL.inferDependent(e)
    println(inferred)
    print(inferred.t)
    util.gen.CProgram(inferred, "Foo_foo")
  }

  test("Dependent pair match with reduction") {
    val e = fun(n2dPairT(n => n`.`f32))(pair =>
      dmatch(pair)(nFun(_ => fun(xs =>
        reduceSeq(fun(x => fun(y => x + y)))(l(0.0f))(xs))
      ))
    )
    val inferred: Expr = TDSL.inferDependent(e)
    println(inferred)
    print(inferred.t)
    util.gen.CProgram(inferred, "Foo_foo")
  }

  test("Dependent pair match with taking") {
    val e = fun(n2dPairT(n => n`.`f32))(pair =>
      dmatch(pair)(nFun(_ => fun(xs => mapSeq(fun(x => x))(take(5)(xs)))))
    )
    val inferred: Expr = TDSL.inferDependent(e)
    println(inferred)
    print(inferred.t)
    util.gen.CProgram(inferred, "Foo_foo")
  }


  test("Simple nested") {
    val e = nFun(n => fun(n `..` (i => (i+1) `.` f32))(array =>
        depMapSeq(nFun(_ => mapSeq(fun(x => x))))(array)
      ))

    val inferred: Expr = TDSL.inferDependent(e)
    println(inferred)
    print(inferred.t)
    util.gen.CProgram(inferred, "Foo_foo")
  }

  test("Simple reduce") {
    val e = nFun(n => fun(n `..` (i => (i+1) `.` f32))(array =>
      depMapSeq(nFun(_ => reduceSeq(fun(x => fun(y => x + y)))(l(0.0f))))(array)
    ))

    val inferred: Expr = TDSL.inferDependent(e)
    println(inferred)
    print(inferred.t)
    util.gen.CProgram(inferred, "Foo_foo")
  }

  test("List of list dot product") {
    val e = nFun(n =>
      fun(n `.` f32)(vector =>
      fun(n `.` NatType)(lengths =>
      fun(n `..` (i => (lengths `#` i) `.` (f32 `x` IndexType(n))))(array => {
        depMapSeq(nFun(_ => fun(
          row =>
            reduceSeq(
              fun(x => fun(y => x + y))
            )(l(0.0f))(mapSeq(fun(entry => {
              val number = entry._1
              val index = entry._2
              number * (vector `@` index)
            }))(row))
        )
        ))(array)
      }
    ))))

    val inferred: Expr = TDSL.inferDependent(e)
    println(inferred)
    print(inferred.t)
    util.gen.CProgram(inferred, "Foo_foo")
  }
}
