// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1.isee

import spinal.core._
import spinal.lib._
import v1.{Frontend, ScalarDecode}
import v1.testram.{TestRam, TestRamConfig}

// =============================================================================
// IseeDemoTop — minimal DUT: Frontend + TestRam + IseeDecode
//
// Generates decode events and sends them via DPI-C to the C backend.
// =============================================================================

class IseeDemoTop(bootromPath: Option[String] = None) extends Component {
  val io = new Bundle {
    val clk   = in Bool()
    val reset = in Bool()
  }

  val coreClockDomain = ClockDomain(clock = io.clk, reset = io.reset)
  val area = new ClockingArea(coreClockDomain) {

    // === Frontend (auto-fetch) ===
    val frontend = new Frontend
    frontend.io.clk        := io.clk
    frontend.io.reset      := io.reset
    frontend.io.sync.flush := False

    // === Rw64Fetch adapter ===
    val rw64 = new v1.Rw64Fetch(v1.Rw64FetchConfig(addrWidth = 64, dataWidth = 64))
    rw64.io.cpu <> frontend.io.toFetch

    // === TestRam (instruction memory) ===
    val testRamConfig = TestRamConfig(width = 8, depth = 2048, initFile = bootromPath)
    val testRam = new TestRam(testRamConfig)

    testRam.io.rw.waddr  := rw64.io.rw.waddr(testRamConfig.addrWidth - 1 downto 0)
    testRam.io.rw.wdata  := rw64.io.rw.wdata
    testRam.io.rw.wvalid := rw64.io.rw.wvalid
    rw64.io.rw.wready := testRam.io.rw.wready
    testRam.io.rw.raddr  := rw64.io.rw.raddr(testRamConfig.addrWidth - 1 downto 0)
    testRam.io.rw.rvalid := rw64.io.rw.rvalid
    rw64.io.rw.rready := testRam.io.rw.rready
    rw64.io.rw.rdata := testRam.io.rw.rdata
    rw64.io.rw.rresp := testRam.io.rw.rresp

    // === ScalarDecode ===
    val decode = new ScalarDecode
    decode.io.inst    := frontend.io.instBits
    decode.io.instIll := frontend.io.instIll

    // === IseeDecode DPI-C blackbox ===
    val isee = new IseeDecode
    // Connect decode signals (<> handles direction: decode.out → isee.in)
    isee.io.clock       := io.clk
    isee.io.valid       <> frontend.io.instValid
    isee.io.pc          <> frontend.io.instPc
    isee.io.instruction := frontend.io.instBits.asUInt
    isee.io.rawInst    <> frontend.io.instRaw
    isee.io.isRVC       <> frontend.io.instIsRVC
    isee.io.ill         <> frontend.io.instIll
    isee.io.legal       <> decode.io.decode.legal
    isee.io.aluOp       <> decode.io.decode.aluOp.asUInt
    isee.io.branch      <> decode.io.decode.branch
    isee.io.jal         <> decode.io.decode.jal
    isee.io.jalr        <> decode.io.decode.jalr
    isee.io.useMem      <> decode.io.decode.useMem
    isee.io.memOp       <> decode.io.decode.memOp.asUInt
    isee.io.useCsr      <> decode.io.decode.useCsr
    isee.io.csrOp       <> decode.io.decode.csrOp.asUInt
  }
}

// =============================================================================
// Generator
// =============================================================================

object GenIseeDemo {
  def main(args: Array[String]): Unit = {
    val bootrom = if (args.length >= 1) Some(args(0)) else None

    SpinalConfig(
      mode = SystemVerilog,
      targetDirectory = "rtl",
      genLineComments = true,
      oneFilePerComponent = true,
      withTimescale = false,
      printFilelist = false
    ).generate {
      new IseeDemoTop(bootrom)
    }
    println("Generated rtl/IseeDemoTop.sv")
  }
}
