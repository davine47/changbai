// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1.demo_dpi

import spinal.core._
import spinal.core.sim._

// =============================================================================
// DemoDpiSim — SpinalSim test demonstrating JNI bridge
//
// Creates a minimal DUT, then calls JNI functions during simulation:
//   compute(a, b)  — native C adds two numbers
//   isLegal(inst)  — native C checks instruction legality
//
// Run:
//   sbt/mill runMain v1.demo_dpi.DemoDpiSim
// =============================================================================

// Minimal DUT for the demo
class DemoDut extends Component {
  val io = new Bundle {
    val clk   = in Bool()
    val reset = in Bool()
    val a     = in UInt(32 bits)
    val b     = in UInt(32 bits)
    val sum   = out UInt(32 bits)  // a + b (combinational)
    val call  = out Bool()         // strobe to trigger JNI call
  }
  io.sum := io.a + io.b
  io.call := True  // always high for demo
}

object DemoDpiSim {
  def main(args: Array[String]): Unit = {

    // 1. Load native library
    val libPath = args.headOption // optional: absolute path to .dylib/.so
    DemoJniBridge.init(libPath)

    // 2. Test JNI directly (no DUT needed)
    println("\n=== Direct JNI Test ===")
    val r1 = DemoJniBridge.compute(3, 4)
    assert(r1 == 7, s"compute(3,4) should be 7, got $r1")
    println(s"[Scala]  compute(3, 4) = $r1  OK")

    val r2 = DemoJniBridge.isLegal(0x7c105073)
    assert(r2 == 1, s"isLegal(0x7c105073) should be 1, got $r2")
    println(s"[Scala]  isLegal(0x7c105073) = $r2  OK")

    val r3 = DemoJniBridge.isLegal(0x00000000)
    assert(r3 == 0, s"isLegal(0x00000000) should be 0, got $r3")
    println(s"[Scala]  isLegal(0x00000000) = $r3  OK")

    // 3. SpinalSim: exercise DUT with JNI calls
    println("\n=== SpinalSim + JNI Test ===")
    SimConfig.withWave.compile(new DemoDut).doSim { dut =>
      dut.clockDomain.forkStimulus(period = 10)

      // Reset
      dut.io.reset #= true
      dut.clockDomain.waitSampling(3)
      dut.io.reset #= false
      dut.clockDomain.waitSampling(1)

      // Drive inputs and call JNI each cycle
      val testCases = Seq(
        (10, 20, 0x7c105073),  // csrwi
        (5, 7, 0x0010041b),    // addiw
        (100, 200, 0x00000000), // illegal (all zeros)
        (0, 0, 0x00100073)     // ebreak
      )

      for ((a, b, inst) <- testCases) {
        dut.io.a #= a
        dut.io.b #= b

        // Call JNI from within SpinalSim
        val hw_sum = DemoJniBridge.compute(a, b)
        val legal = DemoJniBridge.isLegal(inst)

        dut.clockDomain.waitSampling(1)

        // Verify DUT sum matches JNI result
        val dut_sum = dut.io.sum.toLong
        assert(dut_sum == hw_sum.toLong,
          s"Mismatch: DUT sum=$dut_sum, JNI compute=$hw_sum (a=$a, b=$b)")

        println(f"[Sim]   a=$a%3d b=$b%3d  DUT.sum=$dut_sum%3d  JNI.compute=$hw_sum%3d  " +
                f"inst=0x$inst%08X  JNI.isLegal=$legal")
      }

      dut.clockDomain.waitSampling(2)
    }

    println("\n=== Demo DPI/JNI PASSED ===")
  }
}
