// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1

import spinal.core._
import spinal.lib._

// =============================================================================
// InstQueue — instruction buffer queue after RVCDecoder
//
// Input:  RVCDecoder output + fetchData (up to 4 instructions per cycle)
// Output: 1 instruction per cycle (16-bit or 32-bit raw bits + isRVC flag)
//
// Internal:
//   - 4-entry staging registers → push into StreamFifo(N) one by one
//   - StreamFifo provides 1 pop per cycle to the outside
// =============================================================================

class InstQueue(depth: Int = 16) extends Component {
  val io = new Bundle {
    // --- control ---
    val flush = in Bool()  // active high, clear queue and all state

    // --- input from RVCDecoder ---
    val fetchData  = in Bits(64 bits)
    val carryIn    = in Bits(16 bits)
    val hasCarryIn = in Bool()

    val inst0Valid = in Bool()
    val inst0Is32  = in Bool()
    val inst1Valid = in Bool()
    val inst1Is32  = in Bool()
    val inst2Valid = in Bool()
    val inst2Is32  = in Bool()
    val inst3Valid = in Bool()
    val inst3Is32  = in Bool()

    // --- output: 1 instruction per cycle ---
    val instValid = out Bool()
    val instBits  = out Bits(32 bits)  // 32-bit: full; 16-bit: lower 16 bits
    val isRVC     = out Bool()         // true = compressed (16-bit)
  }

  // =========================================================================
  // 1. Slot position calculation (same as RVCDecoder internal logic)
  // =========================================================================
  val i0Slot = Mux(io.hasCarryIn, U(0, 3 bits), U(1, 3 bits))
  val i0Size = Mux(io.inst0Is32, U(2, 3 bits), U(1, 3 bits))
  val i1Slot = i0Slot + i0Size
  val i1Size = Mux(io.inst1Is32, U(2, 3 bits), U(1, 3 bits))
  val i2Slot = i1Slot + i1Size
  val i2Size = Mux(io.inst2Is32, U(2, 3 bits), U(1, 3 bits))
  val i3Slot = i2Slot + i2Size

  // =========================================================================
  // 2. Halfword extraction
  // =========================================================================
  val hw0 = io.carryIn
  val hw1 = io.fetchData(15 downto 0)
  val hw2 = io.fetchData(31 downto 16)
  val hw3 = io.fetchData(47 downto 32)
  val hw4 = io.fetchData(63 downto 48)

  def hwBySlot(slot: UInt): Bits = {
    val ret = Bits(16 bits)
    ret := 0
    switch(slot) {
      is(U(0))(ret := hw0)
      is(U(1))(ret := hw1)
      is(U(2))(ret := hw2)
      is(U(3))(ret := hw3)
      is(U(4))(ret := hw4)
    }
    ret
  }

  // 32-bit: {hw[S+1], hw[S]}
  def raw32(slot: UInt): Bits = hwBySlot(slot + 1) ## hwBySlot(slot)
  // 16-bit: hw[S], padded to 32 bits
  def raw16(slot: UInt): Bits = B"0000000000000000" ## hwBySlot(slot)

  // =========================================================================
  // 3. Extract raw bits + isRVC for 4 instructions
  // =========================================================================
  val i0Bits = Mux(io.inst0Is32, raw32(i0Slot), raw16(i0Slot))
  val i0Rvc  = !io.inst0Is32

  val i1Bits = Mux(io.inst1Is32, raw32(i1Slot), raw16(i1Slot))
  val i1Rvc  = !io.inst1Is32

  val i2Bits = Mux(io.inst2Is32, raw32(i2Slot), raw16(i2Slot))
  val i2Rvc  = !io.inst2Is32

  val i3Bits = Mux(io.inst3Is32, raw32(i3Slot), raw16(i3Slot))
  val i3Rvc  = !io.inst3Is32

  // =========================================================================
  // 4. Staging registers (buffer 0~4, max 4 entries)
  // =========================================================================
  val bufValid = Vec(Reg(Bool()) init False, 4)
  val bufBits  = Vec(Reg(Bits(32 bits)), 4)
  val bufRvc   = Vec(Reg(Bool()), 4)

  // Write staging regs: latch when instXValid=1, clear on flush
  when(io.flush) {
    bufValid.foreach(_ := False)
  }.otherwise {
    when(io.inst0Valid) { bufValid(0) := True;  bufBits(0) := i0Bits; bufRvc(0) := i0Rvc }
    when(io.inst1Valid) { bufValid(1) := True;  bufBits(1) := i1Bits; bufRvc(1) := i1Rvc }
    when(io.inst2Valid) { bufValid(2) := True;  bufBits(2) := i2Bits; bufRvc(2) := i2Rvc }
    when(io.inst3Valid) { bufValid(3) := True;  bufBits(3) := i3Bits; bufRvc(3) := i3Rvc }
  }

  // =========================================================================
  // 5. StreamFifo — configurable depth, absorbs bursts
  // =========================================================================
  // Payload: 33 bits = 32-bit inst bits + 1-bit isRVC
  val fifoPayload = HardType(Bits(33 bits))
  val fifo = StreamFifo(fifoPayload, depth)
  fifo.io.flush := io.flush

  // Push FSM: iterate buf[0..3] and push valid entries one by one.
  // Pushes directly when bufValid set and FIFO ready — 1 cycle per entry.
  val pushIdx = Reg(UInt(2 bits)) init 0

  val fifoIn = Stream(fifoPayload)
  fifo.io.push << fifoIn

  val allEmpty = !(bufValid(0) || bufValid(1) || bufValid(2) || bufValid(3))
  val fifoReady = fifo.io.push.ready

  // Drive FIFO input directly from bufValid and FIFO ready
  fifoIn.valid := bufValid(pushIdx) && fifoReady
  fifoIn.payload := bufBits(pushIdx) ## bufRvc(pushIdx).asBits

  when(io.flush) {
    pushIdx := 0
  }.elsewhen(allEmpty) {
    pushIdx := 0
  }.elsewhen(bufValid(pushIdx) && fifoReady) {
    // Push accepted: clear this entry, advance to next
    bufValid(pushIdx) := False
    pushIdx := pushIdx + 1
  }

  // =========================================================================
  // 6. FIFO output → external interface
  // =========================================================================
  io.instValid := fifo.io.pop.valid
  fifo.io.pop.ready := True  // always ready

  // Unpack payload: bits[32:1] = instBits, bit[0] = isRVC
  val popPayload = fifo.io.pop.payload
  io.instBits := popPayload(32 downto 1)
  io.isRVC    := popPayload(0)
}

// =============================================================================
// Generator
// =============================================================================

object GenInstQueue {
  def main(args: Array[String]): Unit = {
    val depth = if (args.length >= 1) args(0).toInt else 16
    SpinalConfig(
      mode = SystemVerilog,
      targetDirectory = "rtl",
      genLineComments = true,
      oneFilePerComponent = true,
      withTimescale = false,
      printFilelist = false
    ).generate {
      new InstQueue(depth)
    }
    println(s"Generated rtl/InstQueue.sv (depth=$depth)")
  }
}
