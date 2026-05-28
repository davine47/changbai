// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1

import spinal.core._
import spinal.lib._

// =============================================================================
// FrontendMonitor — frontend fetch density recorder
//
// Counters:
//   timeCounter     — increments every clock cycle after reset
//   validInstCounts — increments on instValid=1
//   accGapCycles    — accumulates cycles where instValid=0
//
// Gap distribution histogram:
//   bucketGapCycles[0..13] — count of gaps of length 1..14
//   bucketGapCycles[14]    — count of gaps of length 15+
// =============================================================================

class FrontendMonitor extends Component {
  val io = new Bundle {
    val clk       = in Bool()
    val reset     = in Bool()

    // === instruction input ===
    val instValid = in Bool()
    val instBits  = in Bits(32 bits)

    // === statistics output ===
    val timeCounter     = out UInt(64 bits)
    val validInstCounts = out UInt(64 bits)
    val accGapCycles    = out UInt(64 bits)

    // === gap distribution histogram ===
    val bucketGapCycles = out Vec(UInt(32 bits), 15)
  }

  val coreClockDomain = ClockDomain(clock = io.clk, reset = io.reset)
  val area = new ClockingArea(coreClockDomain) {

    // =====================================================================
    // timeCounter — free-running clock cycle counter
    // =====================================================================
    val timeCounter = Reg(UInt(64 bits)) init 0
    timeCounter := timeCounter + 1

    // =====================================================================
    // validInstCounts — count of valid instructions
    // =====================================================================
    val validInstCounts = Reg(UInt(64 bits)) init 0
    when(io.instValid) {
      validInstCounts := validInstCounts + 1
    }

    // =====================================================================
    // accGapCycles — accumulated gap cycles (cycles without valid inst)
    // =====================================================================
    val accGapCycles = Reg(UInt(64 bits)) init 0
    when(!io.instValid) {
      accGapCycles := accGapCycles + 1
    }

    // =====================================================================
    // bucketGapCycles — gap length histogram (15 buckets of 32-bit)
    //   bucket[0..13]: gaps of length 1..14
    //   bucket[14]:    gaps of length 15+
    // =====================================================================
    val SaturationLen = 14  // bucket[14] = 15+

    // Track current gap length
    val currentGap = Reg(UInt(64 bits)) init 0

    // Histogram buckets
    val bucketGapCycles = Vec(Reg(UInt(32 bits)) init 0, 15)

    when(io.instValid) {
      // Gap ended: record it in histogram
      when(currentGap === 0) {
        // Back-to-back valid instructions: gap=0, skip
      }.elsewhen(currentGap <= SaturationLen) {
        bucketGapCycles((currentGap - 1).resize(4 bits)) :=
          bucketGapCycles((currentGap - 1).resize(4 bits)) + 1
      }.otherwise {
        // gap >= 15: increment saturation bucket
        bucketGapCycles(14) := bucketGapCycles(14) + 1
      }
      currentGap := 0
    }.otherwise {
      currentGap := currentGap + 1
    }

    // =====================================================================
    // Output assignments
    // =====================================================================
    io.timeCounter     := timeCounter
    io.validInstCounts := validInstCounts
    io.accGapCycles    := accGapCycles
    io.bucketGapCycles := bucketGapCycles
  }
}

// =============================================================================
// Generator
// =============================================================================

object GenFrontendMonitor {
  def main(args: Array[String]): Unit = {
    SpinalConfig(
      mode = SystemVerilog,
      targetDirectory = "rtl",
      genLineComments = true,
      oneFilePerComponent = true,
      withTimescale = false,
      printFilelist = false
    ).generate {
      new FrontendMonitor
    }
    println("Generated rtl/FrontendMonitor.sv")
  }
}
