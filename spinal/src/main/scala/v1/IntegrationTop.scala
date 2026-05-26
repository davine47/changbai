// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1

import spinal.core._
import spinal.lib._
import v1.testram.{TestRam, TestRamConfig}

// NOTE: RVCExpander not yet integrated (compilation error), currently supports 32-bit instruction decode only



// =============================================================================
// IntegrationBundle — per-instruction decoded output
// =============================================================================

case class InstDecode() extends Bundle {
  val valid  = Bool()
  val raw    = Bits(32 bits)  // instruction bits
  val decode = Bits(12 bits)  // ScalarDecode output
}

// =============================================================================
// IntegrationTop — fetch + decode + memory pipeline
//
// Data path:
//   fetchData[64] → RVCDecoder → instruction boundary scan
//                              → extract 32-bit instructions
//                              → ScalarDecode × 4
//
// Memory path:
//   CPU req ↔ Rw64Fetch ↔ RW64 bus ↔ TestRam
// =============================================================================

class IntegrationTop extends Component {
  val io = new Bundle {
    val clk   = in Bool()
    val reset = in Bool()

    // === Fetch interface ===
    val fetchData   = in  Bits(64 bits)
    val carryIn     = in  Bits(16 bits)
    val hasCarryIn  = in  Bool()
    val carryOut    = out Bits(16 bits)
    val hasCarryOut = out Bool()
    val instCount   = out UInt(3 bits)

    // Up to 4 decoded instructions per chunk
    val inst0 = out(InstDecode())
    val inst1 = out(InstDecode())
    val inst2 = out(InstDecode())
    val inst3 = out(InstDecode())

    // === CPU Pipeline interface (for memory access) ===
    val cpu_reqAddr   = in  UInt(64 bits)
    val cpu_reqValid  = in  Bool()
    val cpu_reqReady  = out Bool()
    val cpu_reqOpcode = in  Bits(CpuOpcode.width bits)
    val cpu_reqWdata  = in  Bits(64 bits)
    val cpu_respValid = out Bool()
    val cpu_respData  = out Bits(64 bits)
  }

  val ramAddrWidth = 14  // TestRam addr width (log2(2048*8))

  val cd = ClockDomain(clock = io.clk, reset = io.reset)
  val area = new ClockingArea(cd) {

    // =====================================================================
    // 1. RVCDecoder — instruction boundary scanner
    // =====================================================================
    val rvcDecoder = new RVCDecoder
    rvcDecoder.io.fetchData  := io.fetchData
    rvcDecoder.io.carryIn    := io.carryIn
    rvcDecoder.io.hasCarryIn := io.hasCarryIn
    io.carryOut    := rvcDecoder.io.carryOut
    io.hasCarryOut := rvcDecoder.io.hasCarryOut
    io.instCount   := rvcDecoder.io.instCount

    // =====================================================================
    // 2. Instruction slot data extraction (hardware Mux)
    // =====================================================================
    val hw0 = io.carryIn
    val hw1 = io.fetchData(15 downto 0)
    val hw2 = io.fetchData(31 downto 16)
    val hw3 = io.fetchData(47 downto 32)
    val hw4 = io.fetchData(63 downto 48)

    def hwBySlot(slot: UInt): Bits = {
      val ret = Bits(16 bits)
      ret := 0  // default value, prevents latch
      switch(slot) {
        is(U(0))(ret := hw0)
        is(U(1))(ret := hw1)
        is(U(2))(ret := hw2)
        is(U(3))(ret := hw3)
        is(U(4))(ret := hw4)
      }
      ret
    }

    def extract32At(slot: UInt): Bits = hwBySlot(slot + 1) ## hwBySlot(slot)

    // Slot positions
    val i0Slot = Mux(io.hasCarryIn, U(0, 3 bits), U(1, 3 bits))
    val i0Size = Mux(rvcDecoder.io.inst0Is32, U(2, 3 bits), U(1, 3 bits))
    val i1Slot = i0Slot + i0Size
    val i1Size = Mux(rvcDecoder.io.inst1Is32, U(2, 3 bits), U(1, 3 bits))
    val i2Slot = i1Slot + i1Size
    val i2Size = Mux(rvcDecoder.io.inst2Is32, U(2, 3 bits), U(1, 3 bits))
    val i3Slot = i2Slot + i2Size

    // Extracted 32-bit instruction data
    // NOTE: compressed (16-bit) instructions are not expanded yet (RVCExpander WIP)
    val i0Inst32 = extract32At(i0Slot)
    val i1Inst32 = extract32At(i1Slot)
    val i2Inst32 = extract32At(i2Slot)
    val i3Inst32 = extract32At(i3Slot)

    // =====================================================================
    // 3. ScalarDecode × 4 — decode each instruction
    // =====================================================================
    val decode0 = new ScalarDecode
    decode0.io.inst := i0Inst32

    val decode1 = new ScalarDecode
    decode1.io.inst := i1Inst32

    val decode2 = new ScalarDecode
    decode2.io.inst := i2Inst32

    val decode3 = new ScalarDecode
    decode3.io.inst := i3Inst32

    // =====================================================================
    // 4. Assemble output bundles
    // =====================================================================
    def buildInstOut(valid: Bool, raw: Bits, d: ScalarDecodeBundle): InstDecode = {
      val b = InstDecode()
      b.valid  := valid
      b.raw    := raw
      b.decode := d.asBits
      b
    }

    // Valid only if: slot valid AND instruction is 32-bit (compressed not yet supported)
    val i0V = rvcDecoder.io.inst0Valid && rvcDecoder.io.inst0Is32
    val i1V = rvcDecoder.io.inst1Valid && rvcDecoder.io.inst1Is32
    val i2V = rvcDecoder.io.inst2Valid && rvcDecoder.io.inst2Is32
    val i3V = rvcDecoder.io.inst3Valid && rvcDecoder.io.inst3Is32

    io.inst0 := buildInstOut(i0V, i0Inst32, decode0.io.decode)
    io.inst1 := buildInstOut(i1V, i1Inst32, decode1.io.decode)
    io.inst2 := buildInstOut(i2V, i2Inst32, decode2.io.decode)
    io.inst3 := buildInstOut(i3V, i3Inst32, decode3.io.decode)

    // =====================================================================
    // 5. Memory path — Rw64Fetch → TestRam
    // =====================================================================
    val fetchConfig = Rw64FetchConfig(addrWidth = 64, dataWidth = 64)
    val ramConfig   = TestRamConfig(width = 8, depth = 2048)

    val cpuFetch = new Rw64Fetch(fetchConfig)
    val testRam  = new TestRam(ramConfig)

    // CPU pipeline side
    cpuFetch.io.cpu.reqAddr   := io.cpu_reqAddr
    cpuFetch.io.cpu.reqValid  := io.cpu_reqValid
    io.cpu_reqReady           := cpuFetch.io.cpu.reqReady
    cpuFetch.io.cpu.reqLen    := U(3, CpuLen.width bits)
    cpuFetch.io.cpu.reqOpcode := io.cpu_reqOpcode
    cpuFetch.io.cpu.reqWdata  := io.cpu_reqWdata
    cpuFetch.io.cpu.respReady := True

    io.cpu_respValid := cpuFetch.io.cpu.respValid
    io.cpu_respData  := cpuFetch.io.cpu.respData

    // RW64 bus: Rw64Fetch (master, 64-bit addr) → TestRam (slave, 14-bit addr)
    testRam.io.rw.waddr  := cpuFetch.io.rw.waddr(ramAddrWidth - 1 downto 0)
    testRam.io.rw.wdata  := cpuFetch.io.rw.wdata
    testRam.io.rw.wvalid := cpuFetch.io.rw.wvalid
    cpuFetch.io.rw.wready := testRam.io.rw.wready

    testRam.io.rw.raddr  := cpuFetch.io.rw.raddr(ramAddrWidth - 1 downto 0)
    testRam.io.rw.rvalid := cpuFetch.io.rw.rvalid
    cpuFetch.io.rw.rready := testRam.io.rw.rready

    cpuFetch.io.rw.rdata := testRam.io.rw.rdata
    cpuFetch.io.rw.rresp := testRam.io.rw.rresp
  }
}

// =============================================================================
// Generator
// =============================================================================

object GenIntegrationTop {
  def main(args: Array[String]): Unit = {
    SpinalConfig(
      mode = SystemVerilog,
      targetDirectory = "rtl",
      genLineComments = true,
      oneFilePerComponent = true,
      withTimescale = false,
      printFilelist = false
    ).generate {
      new IntegrationTop
    }
    println("Generated rtl/IntegrationTop.sv")
  }
}
