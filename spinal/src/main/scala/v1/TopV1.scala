package v1

import spinal.core._
import spinal.lib._
import v1.testram.{TestRam, TestRamConfig}
import v1.isee.IseeDecode

// =============================================================================
// TopV1 — CPU top level: Frontend + Rw64Fetch + ScalarDecode [+ IseeDecode]
//
// Exposes RW64 bus for external TestRam connection.
// enableIsee: when true, instantiate IseeDecode DPI-C blackbox for diff checking.
// =============================================================================

class TopV1(bootromPath: Option[String] = None, enableIsee: Boolean = false) extends Component {
  val io = new Bundle {
    val clk = in Bool()
    val reset = in Bool()

    // === RW64 fetch interface (connect to TestRam) ===
    val rw = master(new Rw64Bus(64, 64))

    // === instruction output ===
    val instValid = out Bool()
    val instPc   = out UInt(64 bits)
    val instBits  = out Bits(32 bits)
    val instIsRVC = out Bool()
    val instIll   = out Bool()
    val instDecode = out(new ScalarDecodeBundle)

    // === control ===
    val flush = in Bool()
  }
  val coreRootClockDomain = ClockDomain(clock = io.clk, reset = io.reset)

  // =====================================================================
  // Fetch Interface
  // =====================================================================
  val area = new ClockingArea(coreRootClockDomain) {
    val rw64 = new Rw64Fetch(Rw64FetchConfig(addrWidth = 64, dataWidth = 64))

    // Manual pass-through (both are master, same direction)
    io.rw.waddr  := rw64.io.rw.waddr
    io.rw.wdata  := rw64.io.rw.wdata
    io.rw.wvalid := rw64.io.rw.wvalid
    rw64.io.rw.wready := io.rw.wready
    io.rw.raddr  := rw64.io.rw.raddr
    io.rw.rvalid := rw64.io.rw.rvalid
    rw64.io.rw.rready := io.rw.rready
    rw64.io.rw.rdata  := io.rw.rdata
    rw64.io.rw.rresp  := io.rw.rresp

    val frontend = new Frontend

    rw64.io.cpu <> frontend.io.toFetch

    frontend.io.sync.flush := io.flush
    frontend.io.clk := io.clk
    frontend.io.reset := io.reset

    io.instValid := frontend.io.instValid
    io.instPc   := frontend.io.instPc
    io.instBits := frontend.io.instBits
    io.instIsRVC := frontend.io.instIsRVC
    io.instIll := frontend.io.instIll

    val decode = new ScalarDecode

    decode.io.inst    := frontend.io.instBits
    decode.io.instIll := frontend.io.instIll
    io.instDecode <> decode.io.decode

    // ===================================================================
    // IseeDecode — DPI-C compare engine (optional)
    // ===================================================================
    if (enableIsee) {
      val isee = new IseeDecode
      isee.io.clock       := io.clk
      isee.io.valid       <> frontend.io.instValid
      isee.io.pc          <> frontend.io.instPc
      isee.io.instruction := frontend.io.instBits.asUInt
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

}

// =============================================================================
// TopV1Sim — simulation wrapper: TopV1 + TestRam
// =============================================================================

class TopV1Sim(bootromPath: Option[String] = None) extends Component {
  val io = new Bundle {
    val clk   = in Bool()
    val reset = in Bool()
  }

  val coreClockDomain = ClockDomain(clock = io.clk, reset = io.reset)
  val area = new ClockingArea(coreClockDomain) {

    val top = new TopV1(enableIsee = true)
    top.io.clk   := io.clk
    top.io.reset := io.reset
    top.io.flush := False

    val testRamConfig = TestRamConfig(width = 8, depth = 2048, initFile = bootromPath)
    val testRam = new TestRam(testRamConfig)

    testRam.io.rw.waddr  := top.io.rw.waddr(testRamConfig.addrWidth - 1 downto 0)
    testRam.io.rw.wdata  := top.io.rw.wdata
    testRam.io.rw.wvalid := top.io.rw.wvalid
    top.io.rw.wready     := testRam.io.rw.wready
    testRam.io.rw.raddr  := top.io.rw.raddr(testRamConfig.addrWidth - 1 downto 0)
    testRam.io.rw.rvalid := top.io.rw.rvalid
    top.io.rw.rready     := testRam.io.rw.rready
    top.io.rw.rdata      := testRam.io.rw.rdata
    top.io.rw.rresp      := testRam.io.rw.rresp
  }
}

// =============================================================================
// Generators
// =============================================================================

object GenTopV1 {
  def main(args: Array[String]): Unit = {
    val enableIsee = args.contains("--isee")
    SpinalConfig(
      mode = SystemVerilog,
      targetDirectory = "rtl",
      genLineComments = true,
      oneFilePerComponent = true,
      withTimescale = false,
      printFilelist = false
    ).generate {
      new TopV1(enableIsee = enableIsee)
    }
    println("Generated rtl/TopV1.sv")
  }
}

object GenTopV1Sim {
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
      new TopV1Sim(bootrom)
    }
    println("Generated rtl/TopV1Sim.sv")
  }
}