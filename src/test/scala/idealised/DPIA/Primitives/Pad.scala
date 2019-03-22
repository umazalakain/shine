package idealised.DPIA.Primitives

import idealised.OpenCL.SurfaceLanguage.DSL.mapGlobal
import idealised.OpenMP.SurfaceLanguage.DSL.mapPar
import idealised.SurfaceLanguage.DSL._
import idealised.SurfaceLanguage.Semantics.FloatData

import idealised.SurfaceLanguage.Types._
import idealised.util.SyntaxChecker
import idealised.utils.ScalaPatterns
import lift.arithmetic._

import scala.util.Random

class Pad extends idealised.util.Tests {
  test("Simple C pad input and copy") {
    val f = nFun(n => fun(ArrayType(n, float))(xs =>
      xs :>> pad(2, 3, 5.0f) :>> mapSeq(fun(x => x))
    ))

    val p = idealised.C.ProgramGenerator.makeCode(idealised.DPIA.FromSurfaceLanguage(TypeInference(f, Map())))
    val code = p.code
    SyntaxChecker(code)
    println(code)
  }

  test("Simple OpenMP pad input and copy") {
    val f = nFun(n => fun(ArrayType(n, float))( xs =>
      xs :>> pad(2, 3, 5.0f) :>> mapPar(fun(x => x))
    ))

    val p = idealised.OpenMP.ProgramGenerator.makeCode(idealised.DPIA.FromSurfaceLanguage(TypeInference(f, Map())))
    val code = p.code
    SyntaxChecker(code)
    println(code)
  }

  test("Simple OpenCL pad input and copy") {
    val f = nFun(n => fun(ArrayType(n, float))( xs =>
      xs :>> pad(2, 3, 5.0f) :>> mapGlobal(fun(x => x))
    ))

    val p = idealised.OpenCL.KernelGenerator.makeCode(idealised.DPIA.FromSurfaceLanguage(TypeInference(f, Map())))
    val code = p.code
    SyntaxChecker.checkOpenCL(code)
    println(code)
  }

  test("OpenCL Pad only left") {
    val f = nFun(n => fun(ArrayType(n, float))( xs =>
      xs :>> pad(2, 0, 5.0f) :>> mapGlobal(fun(x => x))
    ))

    val p = idealised.OpenCL.KernelGenerator.makeCode(idealised.DPIA.FromSurfaceLanguage(TypeInference(f, Map())))
    val code = p.code
    SyntaxChecker.checkOpenCL(code)
    println(code)
  }

  test("OpenCL Pad only right") {
    val f = nFun(n => fun(ArrayType(n, float))( xs =>
      xs :>> pad(0, 3, 5.0f) :>> mapGlobal(fun(x => x))
    ))

    val p = idealised.OpenCL.KernelGenerator.makeCode(idealised.DPIA.FromSurfaceLanguage(TypeInference(f, Map())))
    val code = p.code
    SyntaxChecker.checkOpenCL(code)
    println(code)
  }

  test("Pad 2D (OpenCL)") {
    import idealised.OpenCL._

    val padAmount = 2
    val padValue = 0.0f

    val f = nFun(
      n => nFun(m =>
        fun(ArrayType(n, ArrayType(m, float)))(xs => xs :>> pad2D(m, Cst(padAmount), Cst(padAmount), FloatData(padValue)) :>> mapSeq(mapSeq(fun(x => x))))
      )
    )


    val p = idealised.OpenCL.KernelGenerator.makeCode(1,1)(idealised.DPIA.FromSurfaceLanguage(TypeInference(f, Map())))
    val kernelF = p.as[ScalaFunction`(`Int `,` Int `,` Array[Array[Float]]`)=>`Array[Float]]
    val code = p.code
    SyntaxChecker.checkOpenCL(code)
    println(code)

    opencl.executor.Executor.loadAndInit()
    val random = new Random()
    val actualN = 2
    val actualM = 2
    val input = Array.fill(actualN)(Array.fill(actualM)(random.nextFloat()))
    val scalaOutput = ScalaPatterns.pad2D(input, padAmount, 0.0f).flatten

    val (kernelOutput, _) = kernelF(actualN `,` actualM `,` input)
    opencl.executor.Executor.shutdown()

    assert(kernelOutput sameElements scalaOutput)
  }
}