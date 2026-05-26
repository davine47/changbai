// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1

import spinal.core._
import spinal.lib._


// =============================================================================
// Frontend — CPU frontend: auto fetch + decode + PC tracking
//
// Internal auto-fetch FSM:
//   Fetches from address 0 after reset. Initiates new fetch when nextPc
//   crosses an 8-byte boundary or when carryOut is asserted.
//
// Data flow:
//   auto fetch → Rw64Fetch → RW64 bus → memory
//   memory response → respData/respValid → RVCDecoder(valid) → InstQueue(flush) →
//                      RVCExpander(combinational) → 32-bit instruction
// =============================================================================

class Frontend extends Component {
  val io = new Bundle {
    val clk   = in Bool()
    val reset = in Bool()

    // === instruction output ===
    val instValid = out Bool()
    val instBits  = out Bits(32 bits)
    val isRVC     = out Bool()
    val nextPc    = out UInt(64 bits)

    // === control ===
    val rvcIll = out Bool()  // RVC illegal instruction flag
    val flush = in Bool()

    // === RW64 bus interface (connect to TestRam) ===
    val rw = master(new Rw64Bus(64, 64))
  }

  val coreClockDomain = ClockDomain(clock = io.clk, reset = io.reset)
  val area = new ClockingArea(coreClockDomain) {

    // =====================================================================
    // Rw64Fetch — CPU request → RW64 bus
    // =====================================================================
    val rwFetch = new Rw64Fetch(Rw64FetchConfig(addrWidth = 64, dataWidth = 64))
    rwFetch.io.cpu.reqLen    := 3  // 8 bytes
    rwFetch.io.cpu.reqOpcode := CpuOpcode.READ
    rwFetch.io.cpu.reqWdata  := 0
    rwFetch.io.cpu.respReady := True

    io.rw.waddr  := rwFetch.io.rw.waddr
    io.rw.wdata  := rwFetch.io.rw.wdata
    io.rw.wvalid := rwFetch.io.rw.wvalid
    rwFetch.io.rw.wready := io.rw.wready
    io.rw.raddr  := rwFetch.io.rw.raddr
    io.rw.rvalid := rwFetch.io.rw.rvalid
    rwFetch.io.rw.rready := io.rw.rready
    rwFetch.io.rw.rdata := io.rw.rdata
    rwFetch.io.rw.rresp := io.rw.rresp

    // =====================================================================
    // Carry pipeline register
    // =====================================================================
    val carryReg    = Reg(Bits(16 bits)) init 0
    val hasCarryReg = Reg(Bool()) init False

    // =====================================================================
    // Auto-fetch FSM — fetch addr = nextPc aligned to 8 bytes
    // =====================================================================
    val nextPcReg = Reg(UInt(64 bits)) init 0
    io.nextPc := nextPcReg

    val fetchAddr = nextPcReg(63 downto 3) @@ U"000"  // 8-byte aligned
    val lastFetchAddr = Reg(UInt(64 bits)) init 0
    val fetchReq      = Reg(Bool()) init False
    val booted        = Reg(Bool()) init False

    val needFetch = (fetchAddr =/= lastFetchAddr) || hasCarryReg

    when(io.flush) {
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
    }.elsewhen(rwFetch.io.cpu.reqReady) {
      fetchReq := False
    }

    rwFetch.io.cpu.reqAddr  := lastFetchAddr
    rwFetch.io.cpu.reqValid := fetchReq

    // =====================================================================
    // RVCDecoder — fetch response → instruction boundary scan
    // =====================================================================
    val decoder = new RVCDecoder
    decoder.io.fetchData  := rwFetch.io.cpu.respData
    val validReg = RegNext(rwFetch.io.cpu.respValid) init False
    decoder.io.valid      := validReg
    decoder.io.carryIn    := carryReg
    decoder.io.hasCarryIn := hasCarryReg

    when(io.flush) {
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
    queue.io.flush       := io.flush
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
    io.isRVC     := queue.io.isRVC
    io.rvcIll    := queue.io.isRVC && expander.io.ill

    // =====================================================================
    // NextPC update — increment per instruction
    // =====================================================================
    when(io.flush) {
      nextPcReg := 0
    }.elsewhen(queue.io.instValid) {
      nextPcReg := nextPcReg + Mux(queue.io.isRVC, U(2), U(4))
    }
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
