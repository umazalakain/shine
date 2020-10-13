package shine.OpenCL.IntermediatePrimitives

import arithexpr.arithmetic.ArithExpr.intToCst
import shine.DPIA.Compilation.TranslationContext
import shine.DPIA.Compilation.TranslationToImperative.acc
import shine.DPIA.DSL.{`new` => _, _}
import shine.DPIA.FunctionalPrimitives.{IndexAsNat, NatAsIndex, Split}
import shine.DPIA.ImperativePrimitives.PairAcc2
import shine.DPIA.Phrases._
import shine.DPIA.Types._
import shine.DPIA._
import shine.OpenCL.AdjustArraySizesForAllocations
import shine.OpenCL.DSL._

object OpenCLSegReduceAtomicWrgI {
  def apply(n: Nat,
            k: Nat,
            m: Nat,
            addrSpace: shine.DPIA.Types.AddressSpace,
            dt: DataType,
            f: Phrase[ExpType ->: ExpType ->: AccType ->: CommType],
            init: Phrase[ExpType],
            in: Phrase[ExpType],
            out: Phrase[ExpType ->: CommType])
           (implicit context: TranslationContext): Phrase[CommType] = {
    val pt = PairType(IndexType(k), dt)
    val o: Nat = n/m

    val adj = AdjustArraySizesForAllocations(init, ArrayType(k, dt), addrSpace)

    comment("oclSegReduceAtomic") `;`
      // Initialize final output array g_output
      `new` (addrSpace) (adj.dt, g_output =>
        acc(init)(adj.accF(g_output.wr)) `;`

          // Declare temporary output array s_data
          // (not used in code, but necessary for MapLocalI,
          //  see OpenCLReduceByIndexLocalI for more info on this problem)
          `new` (addrSpace) (ArrayType(o, pt), s_data =>

            // ********************************************************************
            // First Reduction: Every local thread reduces m elements sequentially.
            // ********************************************************************

            // Declare private variable for the reduction of the current segment
            `new` (AddressSpace.Private) (pt, current_reduction =>
              // Declare private variable for the element of the current for-loop iteration
              `new` (AddressSpace.Private) (pt, current_element =>

              // Process all m (n/m)-sized chunks in parallel with all local threads
              MapLocalI(0)(o, pt, pt,
                λ(expT(ArrayType(m, pt), read))(x => λ(accT(pt))(a =>

                  //TODO: Maybe add padding to avoid bank conflicts

                  // Process first element x[0]
                  acc(x `@` NatAsIndex(m, Natural(0)))(current_reduction.wr) `;`

                      // Loop over the remaining (m - 1) elements
                      `for`(m - 1, i =>
                        // Save current element (x[i + 1]) in current_element
                        acc(x `@` NatAsIndex(m, IndexAsNat(m - 1, i) + Natural(1)))(current_element.wr) `;`

                          // If segment of current_reduction != segment of current_element
                          (`if` (fst(current_reduction.rd) `!:=` fst(current_element.rd))

                            `then` (
                            // => end of current segment reached
                            // Write current_reduction.value into g_output[current_reduction.key]
                            f(g_output.rd `@` fst(current_reduction.rd))
                             (snd(current_reduction.rd))
                             (g_output.wr `@` fst(current_reduction.rd)) `;`

                              // and assign current_element to current_reduction
                              (current_reduction.wr :=| pt | current_element.rd)
                            )

                            // Accumulate the value of current_element into the value of current_reduction
                            `else` f(snd(current_reduction.rd))
                                    (snd(current_element.rd))
                                    (PairAcc2(IndexType(k), dt, current_reduction.wr)))
                      ) `;`

                        (a :=| pt | current_reduction.rd)
                    )),
                Split(m, o, read, pt, in),
                s_data.wr)

            )) `;`

              MapLocalI(0)(o, pt, pt,
                λ(expT(pt, read))(x => λ(accT(pt))(a =>
                  atomicBinOpAssign(dt, addrSpace, f,
                                    g_output.wr `@` fst(x),
                                    snd(x))
                )), s_data.rd, s_data.wr) `;`

          // Final result of the reduction of this workgroup is inside g_output.
          out(adj.exprF(g_output.rd))
      ))
  }
}