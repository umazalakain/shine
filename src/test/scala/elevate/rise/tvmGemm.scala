package elevate.rise

import elevate.core._
import elevate.core.strategies.debug.peek
import elevate.core.strategies.basic._
import elevate.core.strategies.traversal._
import elevate.rise.rules.traversal._
import elevate.rise.rules.algorithmic._
import elevate.rise.rules.lowering._
import elevate.rise.rules.movement._
import elevate.rise.strategies.tiling._
import elevate.rise.strategies.normalForm._
import elevate.rise.strategies.predicate._
import elevate.rise.strategies.traversal._
import shine.test_util
import rise.core.TypedDSL._
import rise.core._
import rise.core.types._
import util.gen


// scalastyle:off
class tvmGemm extends test_util.Tests {

  def toDot(name: String): Strategy[Rise] = peek(x => exprToDot(name, x))
  def exprToDot(name: String, e: Expr): Unit = exprToDot("/home/bastian/development/rewriting/dot", name, e, dotPrinter(_))
  def exprToDot(path: String, name: String, e: Expr, dot: Expr => String): Unit = {
    import java.io._
    import sys.process._

    val w =new PrintWriter(new File(s"$path/$name.dot"))
    w.write(dot(e))
    w.flush()
    w.close()
    s"dot -Tpdf $path/$name.dot -o $path/$name.pdf".!
  }

  val N = 2048
  val mm = infer(
    fun(ArrayType(N, ArrayType(N, f32)))(a =>
      fun(ArrayType(N, ArrayType(N, f32)))(b =>
        map(fun(ak =>
          map(fun(bk =>
            (reduce(add)(l(0.0f)) o
              map(fun(x => fst(x) * snd(x)))) $
              zip(ak, bk))) $ transpose(b))) $ a))
  )

  // utils
  def currentTimeSec: Long = System.currentTimeMillis / 1000

  // *** BASELINE **************************************************************

  //test("baseline -- old") {
  //  val time0 = currentTimeSec
  //  val result = (DFNF `;` topDown.apply(reduceMapFusion) `;` lowerToC) (mm)
  //  val time1 = currentTimeSec
  //  println(time1 - time0)

  //  val time2 = currentTimeSec
  //  println(gen.CProgram(result))
  //  val time3 = currentTimeSec
  //  println(time3 - time2)
  //}

//// ICFP'20 Versions /////////////////////////////////////////////////////////

  // -- BASELINE ---------------------------------------------------------------

  val baseline: Strategy[Rise] = ( DFNF `;`
    reduceMapFusion `@` topDown[Rise])

  test("baseline") {
    gen.CProgram((baseline `;` lowerToC)(mm))
  }

  // -- BLOCKING ---------------------------------------------------------------

  // differences compared to paper:
  // * todo: add 'baseline' reuse to paper
  // * need to fission reduce before `splitting` it
  // * isReduce must be isFullyAppliedReduce
  // * reorder is hardcoded

  val isFullyAppliedReduce: Strategy[Rise] = isApplied(isApplied(isApplied(isReduce)))
  val reorder125634: Strategy[Rise] =
    (mapFBeforeSlide `@` topDown[Rise]) `;;`
    (reduceMapFusion `@` topDown[Rise]) `;;`
    (reduceMapFusion `@` topDown[Rise]) `;;`
    RNF `;` (liftReduce `@` topDown[Rise]) `;;`
    RNF `;` (liftReduce `@` topDown[Rise]) `;;`
    RNF `;` (liftReduce `@` bottomUp[Rise]) `;;`
    RNF `;` (liftReduce `@` bottomUp[Rise])

  val blocking: Strategy[Rise] =
    (baseline `;` // <- not in paper
    (tile(32,32)      `@` outermost(mapNest(2))) `;;`
    (reduceMapFission `@` outermost(isApplied(isApplied(isReduceSeq)))) `;;`
    (splitStrategy(4) `@` innermost(isFullyAppliedReduce)) `;;`
    reorder125634)

  test("blocking") {
    gen.CProgram((blocking `;` lowerToC)(mm))
  }

  // -- VECTORIZATION ----------------------------------------------------------

  // differences compared to paper:
  // * instead of isMap -> isFullyAppliedMap

  val isFullyAppliedMap: Strategy[Rise] = isApplied(isApplied(isMap))

  val vectorization: Strategy[Rise] =
    blocking `;;`
    (vectorize(32) `@` innermost(isApplied(isApplied(isMap))))

  test("vectorization") {
    gen.OpenMPProgram((vectorization `;` lowerToC)(mm))
  }

  // -- LOOP PERMUTATION -------------------------------------------------------

  // differences compared to paper:
  // * see blocking version (different loop perm used here but also hardcoded)

  val reorder125364: Strategy[Rise] =
    (mapFBeforeSlide `@` topDown[Rise]) `;;`
      (reduceMapFusion `@` topDown[Rise]) `;;`
      (reduceMapFusion `@` topDown[Rise]) `;;`
      RNF `;` (liftReduce `@` topDown[Rise]) `;;`
      RNF `;` (liftReduce `@` topDown[Rise]) `;;`
      RNF `;` (liftReduce `@` bottomUp[Rise])

  val loopPerm: Strategy[Rise] = baseline `;`
    (tile(32,32)      `@` outermost(mapNest(2))) `;;`
    (reduceMapFission `@` outermost(isApplied(isApplied(isReduceSeq)))) `;;`
    (splitStrategy(4) `@` innermost(isFullyAppliedReduce)) `;;`
    reorder125364 `;;`
    (vectorize(32) `@` innermost(isFullyAppliedMap))

  test("loop permutation") {
    gen.OpenMPProgram((loopPerm `;` lowerToC)(mm))
  }

  // -- ARRAY PACKING ----------------------------------------------------------

  // difference compared to the paper:
  // * use of inLambda
  // pattern matching RewriteResult
  // * instead of fun(x => e) we build our own lambda with idx

  def storeInMemory(what: Strategy[Rise],
                    how: Strategy[Rise]): Strategy[Rise] = { p =>
    extract(what)(p) >>= how >>= (storedSubExpr => {
      val idx = Identifier(freshName("x"))(extract(what)(p) match {
        case Success(p) => p.t
        case Failure(_)  => return _ => Failure(storeInMemory(what,how))
      })

      val replaced = replaceAll(what, idx)(p) match {
        case Success(p) => p
        case Failure(_)  => return _ => Failure(storeInMemory(what,how))
      }
      Success( toMem(storedSubExpr)
                    (lambda(TDSL(idx),replaced)) )
    })
  }

  def replaceAll(exprPredicate: Strategy[Rise], withExpr: Rise): Strategy[Rise] =
    p => tryAll(exprPredicate `;` insert(withExpr)).apply(p)

  def toMem(e: Rise)(f: TDSL[Lambda]): TDSL[App] = let(f)(e)
  def insert(expr: Rise): Strategy[Rise] = _ => Success(expr)
  def extract(what: Strategy[Rise]): Strategy[Rise] = (expr: Rise) => {
    what(expr).flatMapFailure(_ => expr match {
      case App(f,e)        => extract(what)(f).flatMapFailure(_ => extract(what)(e))
      case Lambda(x, e)    => extract(what)(x).flatMapFailure(_ => extract(what)(e))
      case DepLambda(x, e) => extract(what)(e)
      case _: Identifier      => Failure(extract(what))
      case _: Literal         => Failure(extract(what))
      case _: ForeignFunction => Failure(extract(what))
      case _: Primitive       => Failure(extract(what))
      case _ => ??? // forgot something?
    })
  }

  val isTransposedB: Strategy[Rise] = isApplied(isTranspose)
  val permuteB: Strategy[Rise] =
    splitJoin2(32) `;` DFNF `;` argument(idAfter) `;`
    topDown(liftId) `;` topDown(createTransposePair) `;` RNF `;`
    argument(argument(idAfter)) `;` normalize.apply(liftId) `;`
    topDown(idToCopy)

  val packB: Strategy[Rise] =
    storeInMemory(isTransposedB,
      permuteB `;;` // <- todo: move permuteB here in the paper as well
      (vectorize(32) `@` innermost(isVectorizeablePrimitive)) `;;`
      (parallel      `@` outermost(isApplied(isMap)))
    ) `@` inLambda

  def inLambda(s: Strategy[Rise]): Strategy[Rise] =
    isLambda `;` ((e: Rise) => body(inLambda(s))(e)) <+ s

  val arrayPacking: Strategy[Rise] = packB `;;` loopPerm
  test("array packing") {
    gen.OpenMPProgram((arrayPacking `;` lowerToC)(mm))
  }

  // -- CACHE BLOCKS -----------------------------------------------------------

  // bottomUp actually does not traverse to the deepest due to how 'one' is implemented

  val cacheBlocks: Strategy[Rise] = (
    arrayPacking `;;`toDot("left") `;`
      (unroll `@` bottomUp[Rise])
    //(copyAfterGenerate `@` tryAll[Rise]) //`;;`
      //(copyAfterReduce `@` tryAll[Rise]) `;;` toDot("left")
    )

  test("cache blocks") {
    gen.OpenMPProgram((cacheBlocks `;` lowerToC)(mm))
  }

  // -- PARALLEL ---------------------------------------------------------------

  val par = (
    arrayPacking `;;`
    (parallel `@` outermost(isApplied(isMap))) `;;`
    (unroll `@` bottomUp[Rise])
  )

  test("parallel") {
    gen.OpenMPProgram((par `;` lowerToC)(mm))
  }
}
