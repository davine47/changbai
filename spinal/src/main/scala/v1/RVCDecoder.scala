// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1

import spinal.core._
import spinal.lib._

/**
 * RVCDecoder — instruction boundary scanner for a 64-bit fetch chunk.
 *
 * Scans the halfword slots to determine how many complete instructions
 * are in the chunk, their sizes (16-bit or 32-bit), and whether a
 * 32-bit instruction straddles the chunk boundary.
 *
 * Input:  fetchData[63:0] (from Rw64Fetch respData), valid (from Rw64Fetch respValid),
 *         carryIn[15:0], hasCarryIn
 * Output: instruction boundary information + carryOut (all outputs are 0 when valid=0)
 */
class RVCDecoder extends Component {
  val io = new Bundle {
    val fetchData  = in Bits(64 bits)
    val valid      = in Bool()        // from Rw64Fetch respValid
    val carryIn    = in Bits(16 bits)
    val hasCarryIn = in Bool()

    // Instruction count (0-4 complete instructions)
    val instCount = out UInt(3 bits)

    // Per-instruction: valid, is32bit
    val inst0Valid = out Bool()
    val inst0Is32  = out Bool()
    val inst1Valid = out Bool()
    val inst1Is32  = out Bool()
    val inst2Valid = out Bool()
    val inst2Is32  = out Bool()
    val inst3Valid = out Bool()
    val inst3Is32  = out Bool()

    // Carry to next chunk
    val carryOut   = out Bits(16 bits)
    val hasCarryOut = out Bool()
  }

  // =========================================================================
  // Extract halfwords at 5 fixed slots
  // =========================================================================
  // Slot 0: carryIn (if hasCarryIn)
  // Slot 1: fetchData[15:0]
  // Slot 2: fetchData[31:16]
  // Slot 3: fetchData[47:32]
  // Slot 4: fetchData[63:48]

  val hw = Vec(Bits(16 bits), 5)
  hw(0) := io.carryIn
  hw(1) := io.fetchData(15 downto 0)
  hw(2) := io.fetchData(31 downto 16)
  hw(3) := io.fetchData(47 downto 32)
  hw(4) := io.fetchData(63 downto 48)

  val startSlot = Mux(io.hasCarryIn, U(0, 3 bits), U(1, 3 bits))

  // =========================================================================
  // Classify each halfword: is it a 32-bit instruction start?
  // =========================================================================
  val is32Start = Vec(Bool(), 5)
  for (i <- 0 until 5) {
    is32Start(i) := hw(i)(1 downto 0) === B"11" && hw(i)(4 downto 2) =/= B"111"
  }

  // =========================================================================
  // Boundary scan
  // =========================================================================
  val i0Slot = startSlot
  val i0Is32 = is32Start(i0Slot)
  val i0End  = i0Slot + Mux(i0Is32, U(2), U(1))

  val i1Slot = i0End
  val i1Is32 = is32Start(i1Slot)
  val i1End  = i1Slot + Mux(i1Is32, U(2), U(1))

  val i2Slot = i1End
  val i2Is32 = is32Start(i2Slot)
  val i2End  = i2Slot + Mux(i2Is32, U(2), U(1))

  val i3Slot = i2End
  val i3Is32 = is32Start(i3Slot)
  val i3End  = i3Slot + Mux(i3Is32, U(2), U(1))

  // =========================================================================
  // Validity and straddle detection
  // =========================================================================
  // An instruction is complete if its LAST data slot is within the chunk (≤4)
  def lastSlot(slot: UInt, is32: Bool): UInt = slot + Mux(is32, U(1), U(0))

  val i0Complete = lastSlot(i0Slot, i0Is32) <= 4
  val i1Complete = i0Complete && lastSlot(i1Slot, i1Is32) <= 4
  val i2Complete = i1Complete && lastSlot(i2Slot, i2Is32) <= 4
  val i3Complete = i2Complete && lastSlot(i3Slot, i3Is32) <= 4

  // Straddle: a 32-bit instruction at slot S straddles if S+1 > 4 (needs next chunk)
  // i.e., the second halfword of the instruction is beyond the chunk
  val i0Straddle = i0Is32 && i0Slot + 1 > 4 && i0Slot <= 4
  val i1Straddle = i1Is32 && i1Slot + 1 > 4 && i0Complete && i1Slot <= 4
  val i2Straddle = i2Is32 && i2Slot + 1 > 4 && i1Complete && i2Slot <= 4
  val i3Straddle = i3Is32 && i3Slot + 1 > 4 && i2Complete && i3Slot <= 4

  // =========================================================================
  // Output — gated by valid (from Rw64Fetch respValid)
  // =========================================================================
  io.instCount := Mux(io.valid,
                   Mux(i3Complete, U(4),
                   Mux(i2Complete, U(3),
                   Mux(i1Complete, U(2),
                   Mux(i0Complete, U(1), U(0))))), U(0))

  io.inst0Valid := io.valid && i0Complete
  io.inst1Valid := io.valid && i1Complete
  io.inst2Valid := io.valid && i2Complete
  io.inst3Valid := io.valid && i3Complete

  io.inst0Is32 := i0Is32
  io.inst1Is32 := i1Is32
  io.inst2Is32 := i2Is32
  io.inst3Is32 := i3Is32

  // Carry out
  val straddleSlot = UInt(3 bits)
  val hasStraddle  = Bool()
  hasStraddle := False
  straddleSlot := 0

  when(i0Straddle) {
    hasStraddle := True; straddleSlot := i0Slot
  }.elsewhen(i1Straddle) {
    hasStraddle := True; straddleSlot := i1Slot
  }.elsewhen(i2Straddle) {
    hasStraddle := True; straddleSlot := i2Slot
  }.elsewhen(i3Straddle) {
    hasStraddle := True; straddleSlot := i3Slot
  }

  io.hasCarryOut := io.valid && hasStraddle
  io.carryOut := hw(straddleSlot)
}

/** Generates standalone Verilog for RVCDecoder */
object GenRVCDecoder {
  def main(args: Array[String]): Unit = {
    SpinalConfig(
      mode = SystemVerilog,
      targetDirectory = "rtl",
      genLineComments = true,
      oneFilePerComponent = true,
      withTimescale = false,
      printFilelist = false
    ).generate { new RVCDecoder }
    println("Generated rtl/RVCDecoder.sv")
  }
}
