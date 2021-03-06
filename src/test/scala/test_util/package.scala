import opencl.executor.Executor
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.funsuite.AnyFunSuite
import util.{AssertSame, Time, TimeSpan}

package object test_util {
  abstract class Tests extends AnyFunSuite with Matchers

  abstract class TestsWithExecutor extends Tests with BeforeAndAfter {
    before {
      Executor.loadLibrary()
      Executor.init()
    }

    after {
      Executor.shutdown()
    }
  }

  def runsWithSameResult[R, U <: Time.Unit](runs: Seq[(String, (R, TimeSpan[U]))])
                                           (implicit assertSame: AssertSame[R]): Unit = {
    runs.tail.foreach(r => assertSame(r._2._1, runs.head._2._1, s"${r._1} had a different result"))
    runs.foreach(r => println(s"${r._1} time: ${r._2._2}"))
  }
}
