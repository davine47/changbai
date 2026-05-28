// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1

import spinal.core._
import spinal.lib._

// =============================================================================
// Frontend — CPU frontend: auto fetch + decode + PC tracking
//
// Exposes CpuPipelineBus directly (no internal Rw64Fetch adapter).
//
// Data flow:
//   auto fetch FSM → io.toFetch (CpuPipelineBus, always READ)
//   respData/respValid → RVCDecoder → InstQueue → RVCExpander → 32-bit inst
// =============================================================================

class Frontend extends Component {
  val io = new Bundle {
    val clk   = in Bool()
    val reset = in Bool()
    val toFetch = master(CpuPipelineBus(addrWidth = 64, dataWidth = 64))

    val instValid = out Bool()
    val instBits  = out Bits(32 bits)
    val instIsRVC     = out Bool()
    val instIll = out Bool()

    val sync = new Bundle {
      val flush = in Bool()
    }

  }
  val coreClockDomain = ClockDomain(clock = io.clk, reset = io.reset)

  val area = new ClockingArea(coreClockDomain) {

    // =====================================================================
    // Carry pipeline register
    // =====================================================================
    val carryReg    = Reg(Bits(16 bits)) init 0
    val hasCarryReg = Reg(Bool()) init False

    // =====================================================================
    // Auto-fetch FSM — fetch addr = nextPc aligned to 8 bytes
    // =====================================================================
    val nextPcReg = Reg(UInt(64 bits)) init 0
    val fetchAddr = nextPcReg(63 downto 3) @@ U"000"  // 8-byte aligned
    val lastFetchAddr = Reg(UInt(64 bits)) init 0
    val fetchReq      = Reg(Bool()) init False
    val booted        = Reg(Bool()) init False

    val needFetch = (fetchAddr =/= lastFetchAddr) || hasCarryReg

    when(io.sync.flush) {
      nextPcReg     := 0
      lastFetchAddr := 0
      fetchReq      := False
      booted        := False
    }.elsewhen(!booted) {
      booted        := True
      fetchReq      := True
      lastFetchAddr := 0
    }.elsewhen(!fetchReq) {
      when(needFetch) {
        fetchReq      := True
        lastFetchAddr := fetchAddr
      }
    }.elsewhen(io.toFetch.reqReady) {
      fetchReq := False
    }

    io.toFetch.reqAddr  := lastFetchAddr
    io.toFetch.reqValid := fetchReq

    // =====================================================================
    // RVCDecoder — fetch response → instruction boundary scan
    // =====================================================================
    val decoder = new RVCDecoder
    decoder.io.fetchData  := io.toFetch.respData
    val validReg = RegNext(io.toFetch.respValid) init False
    decoder.io.valid      := validReg
    decoder.io.carryIn    := carryReg
    decoder.io.hasCarryIn := hasCarryReg

    when(io.sync.flush) {
      carryReg    := 0
      hasCarryReg := False
    }.elsewhen(validReg) {
      carryReg    := decoder.io.carryOut
      hasCarryReg := decoder.io.hasCarryOut
    }

    // =====================================================================
    // InstQueue — instruction buffer (4→1 per cycle)
    // =====================================================================
    val queue = new InstQueue(depth = 16)
    queue.io.flush       := io.sync.flush
    queue.io.fetchData   := decoder.io.fetchData
    queue.io.carryIn     := decoder.io.carryIn
    queue.io.hasCarryIn  := decoder.io.hasCarryIn
    queue.io.inst0Valid  := decoder.io.inst0Valid
    queue.io.inst0Is32   := decoder.io.inst0Is32
    queue.io.inst1Valid  := decoder.io.inst1Valid
    queue.io.inst1Is32   := decoder.io.inst1Is32
    queue.io.inst2Valid  := decoder.io.inst2Valid
    queue.io.inst2Is32   := decoder.io.inst2Is32
    queue.io.inst3Valid  := decoder.io.inst3Valid
    queue.io.inst3Is32   := decoder.io.inst3Is32

    // =====================================================================
    // RVCExpander — 16-bit → 32-bit (pure combinational)
    // =====================================================================
    val expander = new RVCExpander
    expander.io.instIn := queue.io.instBits(15 downto 0)

    io.instValid := queue.io.instValid
    io.instBits  := Mux(queue.io.isRVC, expander.io.instOut.bits, queue.io.instBits)
    io.instIsRVC     := queue.io.isRVC
    io.instIll    := queue.io.isRVC && expander.io.ill

    // =====================================================================
    // NextPC update — increment per instruction
    // =====================================================================
    when(io.sync.flush) {
      nextPcReg := 0
    }.elsewhen(queue.io.instValid) {
      nextPcReg := nextPcReg + Mux(queue.io.isRVC, U(2), U(4))
    }

    // =====================================================================
    // CPU request config (always READ, 8 bytes)
    // =====================================================================
    io.toFetch.reqLen    := 3  // 8 bytes
    io.toFetch.reqOpcode := CpuOpcode.READ
    io.toFetch.reqWdata  := 0
    io.toFetch.respReady := True

  }
}

// =============================================================================
// Generator
// =============================================================================

object GenFrontend {
  def main(args: Array[String]): Unit = {
    SpinalConfig(
      mode = SystemVerilog,
      targetDirectory = "rtl",
      genLineComments = true,
      oneFilePerComponent = true,
      withTimescale = false,
      printFilelist = false
    ).generate {
      new Frontend
    }
    println("Generated rtl/Frontend.sv")
  }
}
