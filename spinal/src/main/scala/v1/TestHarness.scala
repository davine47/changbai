// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1

import spinal.core._
import spinal.lib._
import v1.testram.{TestRam, TestRamConfig}

// =============================================================================
// TestHarness — TopV1 + TestRam + FrontendMonitor integration
//
// Topology:
//   TopV1 (Frontend + Rw64Fetch + ScalarDecode) → io.rw → TestRam
//   TopV1.inst* → FrontendMonitor
// =============================================================================

class TestHarness(bootromPath: Option[String] = None) extends Component {
  val io = new Bundle {
    val clk   = in Bool()
    val reset = in Bool()
    val flush = in Bool()

    // === instruction output (from TopV1) ===
    val instValid  = out Bool()
    val instBits   = out Bits(32 bits)
    val instIsRVC  = out Bool()
    val instIll    = out Bool()
    val instEffective = out Bool()  // instValid && instruction is illegal

    // === decode output (from TopV1.instDecode, gated by instValid) ===
    val decLegal      = out Bool()
    val decBranch     = out Bool()
    val decJal        = out Bool()
    val decJalr       = out Bool()
    val decRrf1       = out Bool()
    val decRrf2       = out Bool()
    val decWrf1       = out Bool()
    val decUseALU     = out Bool()
    val decAluOp      = out Bits(5 bits)
    val decUseMem     = out Bool()
    val decMemOp      = out Bits(5 bits)
    val decMemResOp   = out Bits(5 bits)
    val decUseCsr     = out Bool()
    val decCsrOp      = out Bits(5 bits)
    val decNeedImmExt = out Bool()
    val decImmExtType = out Bits(3 bits)
    val decFence      = out Bool()
    val decFenceI     = out Bool()
    val decAmo        = out Bool()

    // === FrontendMonitor output ===
    val monTimeCounter     = out UInt(64 bits)
    val monValidInstCounts = out UInt(64 bits)
    val monAccGapCycles    = out UInt(64 bits)
    val monBucketGapCycles = out Vec(UInt(32 bits), 15)
  }

  val coreClockDomain = ClockDomain(clock = io.clk, reset = io.reset)
  val area = new ClockingArea(coreClockDomain) {

    // =====================================================================
    // TopV1 — CPU core (Frontend + Rw64Fetch + ScalarDecode)
    // =====================================================================
    val top = new TopV1
    top.io.clk   := io.clk
    top.io.reset := io.reset
    top.io.flush := io.flush

    io.instValid := top.io.instValid
    io.instBits  := top.io.instBits
    io.instIsRVC := top.io.instIsRVC
    io.instIll   := top.io.instIll

    // =====================================================================
    // TestRam — RW64 slave memory (instruction store)
    // =====================================================================
    val testRamConfig = TestRamConfig(width = 8, depth = 2048, initFile = bootromPath)
    val testRam = new TestRam(testRamConfig)

    testRam.io.rw.waddr  := top.io.rw.waddr(testRamConfig.addrWidth - 1 downto 0)
    testRam.io.rw.wdata  := top.io.rw.wdata
    testRam.io.rw.wvalid := top.io.rw.wvalid
    top.io.rw.wready := testRam.io.rw.wready

    testRam.io.rw.raddr  := top.io.rw.raddr(testRamConfig.addrWidth - 1 downto 0)
    testRam.io.rw.rvalid := top.io.rw.rvalid
    top.io.rw.rready := testRam.io.rw.rready

    top.io.rw.rdata := testRam.io.rw.rdata
    top.io.rw.rresp := testRam.io.rw.rresp

    // =====================================================================
    // Decode output — pass-through from TopV1, gated by instValid
    // =====================================================================
    val effectiveLegal = top.io.instDecode.legal && !top.io.instIll
    io.instEffective := top.io.instValid && !effectiveLegal

    when(top.io.instValid) {
      io.decLegal      := effectiveLegal
      io.decBranch     := top.io.instDecode.branch
      io.decJal        := top.io.instDecode.jal
      io.decJalr       := top.io.instDecode.jalr
      io.decRrf1       := top.io.instDecode.rrf1
      io.decRrf2       := top.io.instDecode.rrf2
      io.decWrf1       := top.io.instDecode.wrf1
      io.decUseALU     := top.io.instDecode.useALU
      io.decAluOp      := top.io.instDecode.aluOp
      io.decUseMem     := top.io.instDecode.useMem
      io.decMemOp      := top.io.instDecode.memOp
      io.decMemResOp   := top.io.instDecode.memResOp
      io.decUseCsr     := top.io.instDecode.useCsr
      io.decCsrOp      := top.io.instDecode.csrOp
      io.decNeedImmExt := top.io.instDecode.needImmExt
      io.decImmExtType := top.io.instDecode.immExtType
      io.decFence      := top.io.instDecode.fence
      io.decFenceI     := top.io.instDecode.fenceI
      io.decAmo        := top.io.instDecode.amo
    }.otherwise {
      io.decLegal      := False
      io.decBranch     := False
      io.decJal        := False
      io.decJalr       := False
      io.decRrf1       := False
      io.decRrf2       := False
      io.decWrf1       := False
      io.decUseALU     := False
      io.decAluOp      := 0
      io.decUseMem     := False
      io.decMemOp      := 0
      io.decMemResOp   := 0
      io.decUseCsr     := False
      io.decCsrOp      := 0
      io.decNeedImmExt := False
      io.decImmExtType := 0
      io.decFence      := False
      io.decFenceI     := False
      io.decAmo        := False
    }

    // =====================================================================
    // FrontendMonitor — fetch density recorder
    // =====================================================================
    val monitor = new FrontendMonitor
    monitor.io.clk       := io.clk
    monitor.io.reset     := io.reset
    monitor.io.instValid := top.io.instValid
    monitor.io.instBits  := top.io.instBits

    io.monTimeCounter     := monitor.io.timeCounter
    io.monValidInstCounts := monitor.io.validInstCounts
    io.monAccGapCycles    := monitor.io.accGapCycles
    io.monBucketGapCycles := monitor.io.bucketGapCycles
  }
}

// =============================================================================
// Generator
// =============================================================================

object GenTestHarness {
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
      new TestHarness(bootrom)
    }

    println(s"Generated rtl/TestHarness.sv" + bootrom.map(" (bootrom=" + _ + ")").getOrElse(""))
  }
}
