// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1.testram

import spinal.core._
import spinal.lib._
import v1.Rw64Bus

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}
import scala.collection.mutable.ArrayBuffer

// =============================================================================
// Configuration
// =============================================================================

case class TestRamConfig(
    width: Int = 8,
    depth: Int = 2048,
    initFile: Option[String] = None
) {
  val dataWidth: Int = width * 8
  val addrWidth: Int = log2Up(depth * width)
  val rowAddrWidth: Int = log2Up(depth)

  def loadInitContent(): Seq[BigInt] = {
    initFile match {
      case Some(path) =>
        val bytes = Files.readAllBytes(Paths.get(path))
        val words = new ArrayBuffer[BigInt]()
        val bytesPerWord = width
        var i = 0
        while (i + bytesPerWord <= bytes.length) {
          var word: BigInt = 0
          for (b <- (0 until bytesPerWord).reverse) {
            word = (word << 8) | (bytes(i + b).toInt & 0xFF)
          }
          words += word
          i += bytesPerWord
        }
        while (words.length < depth) words += BigInt(0)
        words.toSeq
      case None => Seq.empty
    }
  }
}

// =============================================================================
// TestRam — RW64 protocol slave memory module
// =============================================================================

class TestRam(config: TestRamConfig) extends Component {
  import config._

  val io = new Bundle {
    val rw = slave(new Rw64Bus(addrWidth, dataWidth))
  }

  val byteOffsetBits = log2Up(width)
  val rowAddrBits    = rowAddrWidth

  def byteAddrToRow(addr: UInt): UInt = {
    addr(byteOffsetBits + rowAddrBits - 1 downto byteOffsetBits)
  }

  val mem = Mem(Bits(dataWidth bits), depth)

  val initData = loadInitContent()
  if (initData.nonEmpty) {
    mem.initBigInt(initData)
  }

  io.rw.wready := True
  val wRowAddr = byteAddrToRow(io.rw.waddr)
  mem.write(wRowAddr, io.rw.wdata, enable = io.rw.wvalid && io.rw.wready)

  val readPending = RegInit(False)
  val readRowAddr = Reg(UInt(rowAddrBits bits))
  io.rw.rready := !readPending

  val readDataReg = Reg(Bits(dataWidth bits))
  io.rw.rdata := readDataReg
  io.rw.rresp := readPending

  when(!readPending) {
    when(io.rw.rvalid && io.rw.rready) {
      readPending := True
      readRowAddr := byteAddrToRow(io.rw.raddr)
    }
  }.otherwise {
    readDataReg := mem.readAsync(readRowAddr)
    readPending := False
  }
}

// =============================================================================
// Top-level wrapper
// =============================================================================

class TestRamTop(config: TestRamConfig = TestRamConfig()) extends Component {
  import config._

  val io = new Bundle {
    val clk   = in Bool()
    val reset = in Bool()
    val rw = slave(new Rw64Bus(addrWidth, dataWidth))
  }

  val cd = ClockDomain(clock = io.clk, reset = io.reset)
  val area = new ClockingArea(cd) {
    val ram = new TestRam(config)
    // Slave (ram) ← Master (io): manual connection
    ram.io.rw.waddr  := io.rw.waddr
    ram.io.rw.wdata  := io.rw.wdata
    ram.io.rw.wvalid := io.rw.wvalid
    io.rw.wready     := ram.io.rw.wready
    ram.io.rw.raddr  := io.rw.raddr
    ram.io.rw.rvalid := io.rw.rvalid
    io.rw.rready     := ram.io.rw.rready
    io.rw.rdata      := ram.io.rw.rdata
    io.rw.rresp      := ram.io.rw.rresp
  }
}

// =============================================================================
// Generator
// =============================================================================

object GenTestRam {
  def main(args: Array[String]): Unit = {
    val width   = if (args.length >= 1) args(0).toInt else 8
    val depth   = if (args.length >= 2) args(1).toInt else 2048
    val binFile = if (args.length >= 3) Some(args(2)) else None

    val config = TestRamConfig(width = width, depth = depth, initFile = binFile)

    val spinalConfig = SpinalConfig(
      mode = SystemVerilog,
      targetDirectory = "rtl",
      genLineComments = true,
      oneFilePerComponent = true,
      withTimescale = false,
      printFilelist = false
    )

    spinalConfig.generate { new TestRamTop(config) }

    binFile.foreach { binPath =>
      patchReadmemh("rtl/TestRam.sv", width, depth, binPath)
      val oldBin = new File("rtl/TestRamTop.sv_toplevel_area_ram_mem.bin")
      if (oldBin.exists()) oldBin.delete()
    }

    val initMsg = binFile.map(f => s", initFile=$f").getOrElse("")
    println(s"Generated rtl/TestRamTop.sv (width=$width, depth=$depth$initMsg)")
  }

  def patchReadmemh(svPath: String, width: Int, depth: Int, binPath: String): Unit = {
    val svFile = new File(svPath)
    if (!svFile.exists()) return

    val svContent = new String(Files.readAllBytes(svFile.toPath))
    val pattern = """\$readmemb\("([^"]+)",\s*(\w+)\)""".r
    val m = pattern.findFirstMatchIn(svContent)

    m.foreach { matcher =>
      val oldBinName = matcher.group(1)
      val memName    = matcher.group(2)
      val hexName = oldBinName.stripSuffix(".bin") + ".hex"
      val hexPath = "rtl/" + hexName

      // Convert binary to hex words
      val bytes = Files.readAllBytes(Paths.get(binPath))
      val hexLines = new ArrayBuffer[String]()
      val bPerW = width
      var i = 0
      while (i + bPerW <= bytes.length) {
        var word: BigInt = 0
        var b = bPerW - 1
        while (b >= 0) {
          word = (word << 8) | (bytes(i + b).toInt & 0xFF)
          b -= 1
        }
        val hs = word.toString(16)
        hexLines += ("0" * (bPerW * 2 - hs.length)) + hs
        i += bPerW
      }
      while (hexLines.length < depth) {
        hexLines += "0" * (bPerW * 2)
      }

      // Write hex file
      val pw = new PrintWriter(new File(hexPath))
      hexLines.foreach(pw.println)
      pw.close()

      // Replace $readmemb with $readmemh in Verilog
      val oldLine = matcher.matched
      val newLine = "$readmemh(\"" + hexName + "\", " + memName + ")"
      val patched = svContent.replace(oldLine, newLine)
      Files.write(svFile.toPath, patched.getBytes)

      println(s"[GenTestRam] Patched $$readmemb -> $$readmemh")
      println(s"[GenTestRam] Hex init: $hexPath ($depth words)")
    }
  }
}
