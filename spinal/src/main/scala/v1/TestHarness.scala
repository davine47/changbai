// This file is AI[Claude, high]-generated and manually verified.
package v1

import spinal.core._
import spinal.lib._
import v1.testram.{TestRam, TestRamConfig}

// =============================================================================
// TestHarness — Frontend + ScalarDecode + TestRam integration top
// =============================================================================

class TestHarness(bootromPath: Option[String] = None) extends Component {
  val io = new Bundle {
    val clk   = in Bool()
    val reset = in Bool()

    // === instruction output (Frontend) ===
    val instValid = out Bool()
    val instBits  = out Bits(32 bits)
    val isRVC     = out Bool()
    val nextPc    = out UInt(64 bits)

    // === decode output (ScalarDecode) ===
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

    val flush = in Bool()
  }

  val ramAddrWidth = 14

  val coreClockDomain = ClockDomain(clock = io.clk, reset = io.reset)
  val area = new ClockingArea(coreClockDomain) {

    val frontend = new Frontend
    frontend.io.clk   := io.clk
    frontend.io.reset := io.reset
    frontend.io.flush := io.flush

    io.instValid := frontend.io.instValid
    io.instBits  := frontend.io.instBits
    io.isRVC     := frontend.io.isRVC
    io.nextPc    := frontend.io.nextPc

    // =====================================================================
    // ScalarDecode — decode integration
    // =====================================================================
    val scalarDecode = new ScalarDecode
    scalarDecode.io.inst := frontend.io.instBits

    // Output decode result only when instruction is valid, otherwise output all zeros.
    // decLegal = scalarDecode legal AND NOT (RVC illegal)
    // RVCExpander may expand illegal C instructions into valid 32b (e.g. 0x0000 → 0x00010413)
    // ScalarDecode would see a valid instruction, so we must override via rvcIll
    val effectiveLegal = scalarDecode.io.decode.legal && !frontend.io.rvcIll

    when(frontend.io.instValid) {
      io.decLegal      := effectiveLegal
      io.decBranch     := scalarDecode.io.decode.branch
      io.decJal        := scalarDecode.io.decode.jal
      io.decJalr       := scalarDecode.io.decode.jalr
      io.decRrf1       := scalarDecode.io.decode.rrf1
      io.decRrf2       := scalarDecode.io.decode.rrf2
      io.decWrf1       := scalarDecode.io.decode.wrf1
      io.decUseALU     := scalarDecode.io.decode.useALU
      io.decAluOp      := scalarDecode.io.decode.aluOp
      io.decUseMem     := scalarDecode.io.decode.useMem
      io.decMemOp      := scalarDecode.io.decode.memOp
      io.decMemResOp   := scalarDecode.io.decode.memResOp
      io.decUseCsr     := scalarDecode.io.decode.useCsr
      io.decCsrOp      := scalarDecode.io.decode.csrOp
      io.decNeedImmExt := scalarDecode.io.decode.needImmExt
      io.decImmExtType := scalarDecode.io.decode.immExtType
      io.decFence      := scalarDecode.io.decode.fence
      io.decFenceI     := scalarDecode.io.decode.fenceI
      io.decAmo        := scalarDecode.io.decode.amo
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
    // TestRam
    // =====================================================================
    val testRam = new TestRam(TestRamConfig(width = 8, depth = 2048, initFile = bootromPath))

    testRam.io.rw.waddr  := frontend.io.rw.waddr(ramAddrWidth - 1 downto 0)
    testRam.io.rw.wdata  := frontend.io.rw.wdata
    testRam.io.rw.wvalid := frontend.io.rw.wvalid
    frontend.io.rw.wready := testRam.io.rw.wready

    testRam.io.rw.raddr  := frontend.io.rw.raddr(ramAddrWidth - 1 downto 0)
    testRam.io.rw.rvalid := frontend.io.rw.rvalid
    frontend.io.rw.rready := testRam.io.rw.rready

    frontend.io.rw.rdata := testRam.io.rw.rdata
    frontend.io.rw.rresp := testRam.io.rw.rresp
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

    bootrom.foreach { path =>
      patchReadmemh("rtl/TestRam.sv", 8, 2048, path)
    }

    println(s"Generated rtl/TestHarness.sv" + bootrom.map(" (bootrom=" + _ + ")").getOrElse(""))
  }

  def patchReadmemh(svPath: String, width: Int, depth: Int, binPath: String): Unit = {
    import java.io.{File, PrintWriter}
    import java.nio.file.{Files, Paths}
    import scala.collection.mutable.ArrayBuffer

    val svFile = new File(svPath)
    if (!svFile.exists()) return
    val svContent = new String(Files.readAllBytes(svFile.toPath))
    val pattern = """\$readmemb\("([^"]+)",\s*(\w+)\)""".r
    pattern.findFirstMatchIn(svContent).foreach { m =>
      val hexName = m.group(1).stripSuffix(".bin") + ".hex"
      val hexPath = "rtl/" + hexName
      val bytes = Files.readAllBytes(Paths.get(binPath))
      val hexLines = new ArrayBuffer[String]()
      var i = 0
      while (i + width <= bytes.length) {
        var word: BigInt = 0
        var b = width - 1
        while (b >= 0) { word = (word << 8) | (bytes(i + b).toInt & 0xFF); b -= 1 }
        val hs = word.toString(16)
        hexLines += ("0" * (width * 2 - hs.length)) + hs
        i += width
      }
      while (hexLines.length < depth) hexLines += "0" * (width * 2)
      val pw = new PrintWriter(new File(hexPath))
      hexLines.foreach(pw.println)
      pw.close()
      val patched = svContent.replace(m.matched, "$readmemh(\"" + hexName + "\", " + m.group(2) + ")")
      Files.write(svFile.toPath, patched.getBytes)
      val oldBin = new File("rtl/" + m.group(1))
      if (oldBin.exists()) oldBin.delete()
      println("[GenTestHarness] Patched $readmemb -> $readmemh, hex: " + hexPath)
    }
  }
}
