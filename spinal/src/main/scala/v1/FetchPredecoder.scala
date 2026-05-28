// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1

import spinal.core._
import spinal.lib._

/**
 * Fetch Predecoder — identifies instruction boundaries in a 64-bit fetch chunk.
 *
 * ## Problem
 * When fetching 64-bit aligned chunks, RISC-V compressed (16-bit) instructions
 * can appear at any 2-byte boundary. A 32-bit instruction may straddle two
 * fetch chunks. This module determines:
 *   - How many complete instructions are in the current chunk
 *   - Whether a 32-bit instruction crosses into the next chunk (carry)
 *
 * ## Algorithm
 *
 * The 64-bit chunk has 4 halfwords at fixed positions:
 *   H0 = data[15:0],  H1 = data[31:16]
 *   H2 = data[47:32], H3 = data[63:48]
 *
 * Plus an optional carry-in halfword C from the previous chunk.
 *
 * Each halfword H is classified by its inst[1:0]:
 *   - != 11 → 16-bit compressed instruction
 *   - == 11 && inst[4:2] != 111 → 32-bit instruction starting here
 *
 * Scanning from the first valid halfword:
 *   if 16-bit: consumes this halfword, next starts at next halfword
 *   if 32-bit: consumes this halfword + next halfword, next starts 2 halfwords later
 *
 * Straddle: a 32-bit instruction starting at H3 needs H0 from the next chunk.
 * The lower 16 bits (H3) are saved as carryOut for the next cycle.
 *
 * ## Interface
 *   - fetchData[63:0]: current aligned fetch chunk
 *   - carryIn[15:0], hasCarryIn: partial instruction from previous chunk
 *   - instCount[2:0]: number of complete instructions (0-4)
 *   - inst*: per-instruction valid/size/data
 *   - carryOut[15:0], hasCarryOut: for next chunk
 */
class FetchPredecoder extends Component {
  val io = new Bundle {
    // Input
    val fetchData  = in Bits(64 bits)
    val carryIn    = in Bits(16 bits)
    val hasCarryIn = in Bool()

    // Output: instruction count (0-4)
    val instCount = out UInt(3 bits)

    // Per-instruction info
    val inst0Valid = out Bool()
    val inst0Size  = out Bool()       // 0=16b, 1=32b
    val inst0Data  = out Bits(32 bits) // lower 16 bits for 16b, full 32 bits for 32b

    val inst1Valid = out Bool()
    val inst1Size  = out Bool()
    val inst1Data  = out Bits(32 bits)

    val inst2Valid = out Bool()
    val inst2Size  = out Bool()
    val inst2Data  = out Bits(32 bits)

    val inst3Valid = out Bool()
    val inst3Size  = out Bool()
    val inst3Data  = out Bits(32 bits)

    // Carry to next chunk
    val carryOut   = out Bits(16 bits)
    val hasCarryOut = out Bool()
  }

  // =========================================================================
  // Extract halfwords at fixed positions
  // =========================================================================
  // There are 5 possible "slots" in the effective data stream:
  //   Slot 0: carryIn (if hasCarryIn)
  //   Slot 1: fetchData[15:0]   (= H0, or H1 if carry present)
  //   Slot 2: fetchData[31:16]
  //   Slot 3: fetchData[47:32]
  //   Slot 4: fetchData[63:48]

  val hw = Vec(Bits(16 bits), 5)
  hw(0) := io.carryIn
  hw(1) := io.fetchData(15 downto 0)
  hw(2) := io.fetchData(31 downto 16)
  hw(3) := io.fetchData(47 downto 32)
  hw(4) := io.fetchData(63 downto 48)

  // Starting slot: 0 if hasCarryIn, else 1 (first fetch halfword)
  val startSlot = Mux(io.hasCarryIn, U(0, 3 bits), U(1, 3 bits))

  // =========================================================================
  // Classify each halfword
  // =========================================================================
  // is32Start[i]: halfword i is the start of a 32-bit instruction
  val is32Start = Vec(Bool(), 5)
  for (i <- 0 until 5) {
    is32Start(i) := hw(i)(1 downto 0) === B"11" && hw(i)(4 downto 2) =/= B"111"
  }

  // =========================================================================
  // Determine instruction boundaries
  // =========================================================================
  // Instruction 0: always starts at startSlot
  val i0Slot = startSlot
  val i0Is32 = is32Start(i0Slot)
  val i0End  = i0Slot + Mux(i0Is32, U(2), U(1))  // next slot after this instruction

  // Instruction 1: starts after instruction 0
  val i1Slot = i0End
  val i1Is32 = is32Start(i1Slot)
  val i1End  = i1Slot + Mux(i1Is32, U(2), U(1))

  // Instruction 2
  val i2Slot = i1End
  val i2Is32 = is32Start(i2Slot)
  val i2End  = i2Slot + Mux(i2Is32, U(2), U(1))

  // Instruction 3
  val i3Slot = i2End
  val i3Is32 = is32Start(i3Slot)
  val i3End  = i3Slot + Mux(i3Is32, U(2), U(1))

  // =========================================================================
  // Validity: an instruction is valid if it fits entirely within slots 0-4
  // and doesn't start a straddle
  // =========================================================================
  // An instruction fits if its end slot <= 4 (within the chunk) OR
  // if it's the last slot and it's a 32-bit that needs slot 5 (straddle case).
  // For validity (complete), we require end slot <= 4.

  val i0Fits  = i0End <= 5  // slot 5 = beyond chunk (straddle)
  val i1Fits  = i1End <= 5 && i0Fits && !(i0Is32 && i0End > 4)
  val i2Fits  = i2End <= 5 && i1Fits && !(i1Is32 && i1End > 4)
  val i3Fits  = i3End <= 5 && i2Fits && !(i2Is32 && i2End > 4)

  // Straddle: a 32-bit instruction that extends beyond slot 4
  val i0Straddle = i0Is32 && i0End > 4 && i0End <= 5
  val i1Straddle = i1Is32 && i1End > 4 && i1End <= 5 && i0Fits && !i0Straddle
  val i2Straddle = i2Is32 && i2End > 4 && i2End <= 5 && i1Fits && !i1Straddle
  val i3Straddle = i3Is32 && i3End > 4 && i3End <= 5 && i2Fits && !i2Straddle

  // An instruction is "complete" (valid output) if it fits AND doesn't straddle
  val i0Complete = i0End <= 4  // fits entirely in the chunk
  val i1Complete = i1End <= 4 && i0Complete
  val i2Complete = i2End <= 4 && i1Complete
  val i3Complete = i3End <= 4 && i2Complete

  // Count complete instructions
  io.instCount := Mux(i3Complete, U(4),
                   Mux(i2Complete, U(3),
                   Mux(i1Complete, U(2),
                   Mux(i0Complete, U(1), U(0)))))

  // =========================================================================
  // Per-instruction outputs
  // =========================================================================
  io.inst0Valid := i0Complete
  io.inst1Valid := i1Complete
  io.inst2Valid := i2Complete
  io.inst3Valid := i3Complete

  io.inst0Size := i0Is32
  io.inst1Size := i1Is32
  io.inst2Size := i2Is32
  io.inst3Size := i3Is32

  // Instruction data extraction
  def extractInst(slot: UInt, is32: Bool): Bits = {
    val data = Bits(32 bits)
    when(is32) {
      // 32-bit: concatenate two consecutive halfwords
      data := hw(slot + 1) ## hw(slot)
    }.otherwise {
      // 16-bit: zero-extend to 32 bits
      data := B(0, 16 bits) ## hw(slot)
    }
    data
  }

  io.inst0Data := extractInst(i0Slot, i0Is32)
  io.inst1Data := extractInst(i1Slot, i1Is32)
  io.inst2Data := extractInst(i2Slot, i2Is32)
  io.inst3Data := extractInst(i3Slot, i3Is32)

  // =========================================================================
  // Carry out
  // =========================================================================
  // If a 32-bit instruction straddles (needs one more halfword from next chunk),
  // save its lower 16 bits as carryOut.
  val straddleSlot = UInt(3 bits)
  val hasStraddle  = Bool()

  hasStraddle := False
  straddleSlot := 0

  when(i0Straddle) {
    hasStraddle := True
    straddleSlot := i0Slot
  }.elsewhen(i1Straddle) {
    hasStraddle := True
    straddleSlot := i1Slot
  }.elsewhen(i2Straddle) {
    hasStraddle := True
    straddleSlot := i2Slot
  }.elsewhen(i3Straddle) {
    hasStraddle := True
    straddleSlot := i3Slot
  }

  io.hasCarryOut := hasStraddle

  // carryOut = hw[straddleSlot] (the lower 16 bits of the straddling instruction)
  // This is the first halfword of the 32-bit instruction
  io.carryOut := hw(straddleSlot)
}

// =============================================================================
// Generator
// =============================================================================

/**
 * Generates standalone Verilog for the FetchPredecoder module.
 *
 * Usage:
 *   mill -i changbaiV1.spinal.runMain v1.fetch.GenFetchPredecoder
 *
 * Output: changbai/rtl/FetchPredecoder.sv
 */
object GenFetchPredecoder {
  def main(args: Array[String]): Unit = {
    SpinalConfig(
      mode = SystemVerilog,
      targetDirectory = "rtl",
      genLineComments = true,
      oneFilePerComponent = true,
      withTimescale = false,
      printFilelist = false
    ).generate {
      new FetchPredecoder
    }
    println("Generated rtl/FetchPredecoder.sv")
  }
}
