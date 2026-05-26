// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1

import spinal.core._
import spinal.lib._

/**
 * Regfile configuration.
 *
 * @param readPorts   number of read ports (default 2 for standard RISC-V decode)
 * @param writePorts  number of write ports (default 1)
 * @param xlen        register width in bits (32 or 64, default 64 for RV64)
 * @param numRegs     number of registers (default 32 for RV32I/RV64I)
 */
case class RegfileConfig(
    readPorts: Int  = 2,
    writePorts: Int = 1,
    xlen: Int       = 64,
    numRegs: Int    = 32
) {
  def addrWidth: Int = log2Up(numRegs)
}

/**
 * RISC-V register file — 32 architectural registers, x0 hardwired to zero.
 *
 * Implementation uses Vec(Reg) (flip-flop based) to support configurable
 * read and write port counts for superscalar designs.
 *
 * Each write port has independent address/data/enable. When multiple
 * write ports target the same register in the same cycle, the
 * highest-indexed write port wins (last-write-wins arbitration).
 *
 * x0 (register 0) is hardwired to zero: reads always return 0, writes are
 * silently ignored.
 */
class Regfile(config: RegfileConfig) extends Component {
  import config._

  require(readPorts >= 1, "Regfile must have at least 1 read port")
  require(writePorts >= 1, "Regfile must have at least 1 write port")
  require(numRegs == 32 || numRegs == 16, "numRegs must be 16 (RV32E) or 32 (standard)")
  require(xlen == 32 || xlen == 64, "xlen must be 32 or 64")

  val io = new Bundle {
    val readAddr  = Vec(in UInt(addrWidth bits), readPorts)
    val readData  = Vec(out Bits(xlen bits), readPorts)
    val writeAddr = Vec(in UInt(addrWidth bits), writePorts)
    val writeData = Vec(in Bits(xlen bits), writePorts)
    val writeEn   = Vec(in Bool(), writePorts)
  }

  // =========================================================================
  // Register file storage: Vec(Reg) for configurable multi-port support
  // =========================================================================
  // x0 is always zero and not physically stored (31 entries: index 0 = x1, ... index 30 = x31)
  val regs = Vec(Reg(Bits(xlen bits)) init B(0, xlen bits), numRegs - 1)

  // =========================================================================
  // Write logic — one process per register, with write-port arbitration
  // =========================================================================
  // For each physical register (x1..x31), collect all write-port candidates
  // and select the highest-priority (highest index) enabled write.
  for (i <- 1 until numRegs) {
    val addrMatch = Bits(writePorts bits)
    for (wp <- 0 until writePorts) {
      addrMatch(wp) := io.writeEn(wp) && io.writeAddr(wp) === U(i, addrWidth bits)
    }

    val hasWrite = addrMatch.orR

    // Priority encoder: highest port index wins
    // OHToUInt gives lowest set bit; we want highest -> reverse the bits
    val reversedBits = addrMatch.asBools.reverse
    val highestReversedIdx = OHToUInt(reversedBits.asBits)
    val winnerPort = U(writePorts - 1) - highestReversedIdx

    when(hasWrite) {
      regs(i - 1) := io.writeData(winnerPort)
    }
  }

  // =========================================================================
  // Read ports — combinational mux from register array
  // =========================================================================
  for (rp <- 0 until readPorts) {
    val addrOh = UIntToOh(io.readAddr(rp), numRegs)
    val selBits = addrOh(numRegs - 1 downto 1).asBools
    val regVec = Vec(regs.map(r => r.asBits))
    val muxed = MuxOH(selBits, regVec)
    // x0 (address 0) always reads zero
    io.readData(rp) := Mux(io.readAddr(rp) === 0, B(0, xlen bits), muxed)
  }
}

// =============================================================================
// Top-level wrapper + Generator
// =============================================================================

/**
 * Top-level wrapper for standalone Verilog generation with flat I/O.
 */
class RegfileTop(config: RegfileConfig = RegfileConfig()) extends Component {
  import config._

  val io = new Bundle {
    val clk   = in Bool()
    val reset = in Bool()
    val readAddr  = Vec(in UInt(addrWidth bits), readPorts)
    val readData  = Vec(out Bits(xlen bits), readPorts)
    val writeAddr = Vec(in UInt(addrWidth bits), writePorts)
    val writeData = Vec(in Bits(xlen bits), writePorts)
    val writeEn   = Vec(in Bool(), writePorts)
  }

  val cd = ClockDomain(clock = io.clk, reset = io.reset)
  val area = new ClockingArea(cd) {
    val rf = new Regfile(config)
    for (i <- 0 until readPorts) {
      rf.io.readAddr(i) := io.readAddr(i)
      io.readData(i)    := rf.io.readData(i)
    }
    for (i <- 0 until writePorts) {
      rf.io.writeAddr(i) := io.writeAddr(i)
      rf.io.writeData(i) := io.writeData(i)
      rf.io.writeEn(i)   := io.writeEn(i)
    }
  }
}

/**
 * Generates standalone Verilog/SystemVerilog for the Regfile module.
 *
 * Usage:
 *   mill -i changbaiV1.spinal.runMain v1.regfile.GenRegfile
 *
 * Output: changbai/rtl/RegfileTop.sv
 */
object GenRegfile {
  def main(args: Array[String]): Unit = {
    val config = RegfileConfig(
      readPorts  = if (args.length >= 1) args(0).toInt else 2,
      writePorts = if (args.length >= 2) args(1).toInt else 1,
      xlen       = if (args.length >= 3) args(2).toInt else 64,
      numRegs    = if (args.length >= 4) args(3).toInt else 32
    )

    SpinalConfig(
      mode = SystemVerilog,
      targetDirectory = "rtl",
      genLineComments = true,
      oneFilePerComponent = true,
      withTimescale = false,
      printFilelist = false
    ).generate {
      new RegfileTop(config)
    }

    println(s"Generated rtl/RegfileTop.sv (${config.readPorts}R${config.writePorts}W, XLEN=${config.xlen})")
  }
}
