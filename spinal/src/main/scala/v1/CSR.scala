// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1

import spinal.core._
import spinal.lib._

/**
 * RISC-V 64 Machine-mode CSR register addresses.
 * Reference: RISC-V Privileged Architecture Manual, Chapter "Machine-Level CSRs".
 */
object CSRs {
  // Machine Information Registers (read-only)
  val Mvendorid  = 0xF11
  val Marchid    = 0xF12
  val Mimpid     = 0xF13
  val Mhartid    = 0xF14
  val Mconfigptr = 0xF15

  // Machine Trap Setup
  val Mstatus    = 0x300
  val Misa       = 0x301
  val Medeleg    = 0x302
  val Mideleg    = 0x303
  val Mie        = 0x304
  val Mtvec      = 0x305
  val Mcounteren = 0x306

  // Machine Trap Handling
  val Mscratch   = 0x340
  val Mepc       = 0x341
  val Mcause     = 0x342
  val Mtval      = 0x343
  val Mip        = 0x344
}

/**
 * CSR register module — implements RV64 machine-mode Control and Status Registers.
 *
 * The interface provides a simple read/write access port:
 *   - cmd:  input command (addr, wdata, write enable)
 *   - rsp:  output response (rdata, valid, illegal)
 *
 * Supported CSRs (first version, read/write only):
 *   Machine Information: mvendorid, marchid, mimpid, mhartid, mconfigptr (RO)
 *   Machine Trap Setup:  mstatus, misa, medeleg, mideleg, mie, mtvec, mcounteren
 *   Machine Trap Handling: mscratch, mepc, mcause, mtval, mip
 *
 * Access to unimplemented CSR addresses returns 0 and asserts illegal.
 */
class CSR extends Component {
  val io = new Bundle {
    val cmd = new Bundle {
      val valid = in Bool()
      val addr  = in UInt(12 bits)
      val wdata = in Bits(64 bits)
      val wen   = in Bool()  // write enable
    }
    val rsp = new Bundle {
      val rdata   = out Bits(64 bits)
      val valid   = out Bool()  // high for 1 cycle when response is ready
      val illegal = out Bool()
    }
  }

  // =========================================================================
  // CSR State Registers (all 64-bit for RV64)
  // =========================================================================

  // Machine Information Registers (read-only, hardwired constants)
  val mvendorid  = B(0, 64 bits)  // 0 = non-commercial
  val marchid    = B(1, 64 bits)  // architecture ID
  val mimpid     = B(0, 64 bits)  // implementation ID
  val mhartid    = B(0, 64 bits)  // hart ID
  val mconfigptr = B(0, 64 bits)  // config pointer

  // Machine Trap Setup
  // mstatus: simplified — MPP=11 (Machine), default after reset
  val mstatus = Reg(Bits(64 bits)) init(BigInt("0000000a00000000", 16))
  // misa: MXL=2 (RV64, bits 63:62 = 10), extensions: I(8), M(12), C(2), U(20), S(18)
  // extensions = 0x141125 (bits 25:0), full value = 0x8000000000141125
  val misa = Reg(Bits(64 bits)) init(BigInt("8000000000141125", 16))
  val medeleg    = Reg(Bits(64 bits)) init B(0, 64 bits)
  val mideleg    = Reg(Bits(64 bits)) init B(0, 64 bits)
  val mie        = Reg(Bits(64 bits)) init B(0, 64 bits)
  val mtvec      = Reg(Bits(64 bits)) init B(0, 64 bits)
  val mcounteren = Reg(Bits(64 bits)) init B(0, 64 bits)

  // Machine Trap Handling
  val mscratch = Reg(Bits(64 bits)) init B(0, 64 bits)
  val mepc     = Reg(Bits(64 bits)) init B(0, 64 bits)
  val mcause   = Reg(Bits(64 bits)) init B(0, 64 bits)
  val mtval    = Reg(Bits(64 bits)) init B(0, 64 bits)
  val mip      = Reg(Bits(64 bits)) init B(0, 64 bits)

  // =========================================================================
  // Read/Write Logic
  // =========================================================================

  // Default: no access, illegal
  val rdata     = Bits(64 bits)
  val illegal   = Bool()
  rdata   := 0
  illegal := False

  when(io.cmd.valid) {
    // By default, access is legal for implemented CSRs
    // Illegal for unimplemented addresses
    illegal := True
    rdata   := 0

    switch(io.cmd.addr) {
      // ---- Machine Information Registers (read-only) ----
      is(CSRs.Mvendorid) {
        illegal := io.cmd.wen  // writes are illegal
        rdata := mvendorid
      }
      is(CSRs.Marchid) {
        illegal := io.cmd.wen
        rdata := marchid
      }
      is(CSRs.Mimpid) {
        illegal := io.cmd.wen
        rdata := mimpid
      }
      is(CSRs.Mhartid) {
        illegal := io.cmd.wen
        rdata := mhartid
      }
      is(CSRs.Mconfigptr) {
        illegal := io.cmd.wen
        rdata := mconfigptr
      }

      // ---- Machine Trap Setup (read/write) ----
      is(CSRs.Mstatus) {
        illegal := False
        rdata := mstatus
        when(io.cmd.wen) { mstatus := io.cmd.wdata.asBits }
      }
      is(CSRs.Misa) {
        // misa is WARL — writes allowed but may be modified
        illegal := False
        rdata := misa
        when(io.cmd.wen) {
          // Preserve MXL (bits 63:62), allow extension bits to be modified
          misa := (io.cmd.wdata.asBits & (B"11" << 62)) | (io.cmd.wdata.asBits & ~(B"11" << 62))
        }
      }
      is(CSRs.Medeleg) {
        illegal := False
        rdata := medeleg
        when(io.cmd.wen) { medeleg := io.cmd.wdata.asBits }
      }
      is(CSRs.Mideleg) {
        illegal := False
        rdata := mideleg
        when(io.cmd.wen) { mideleg := io.cmd.wdata.asBits }
      }
      is(CSRs.Mie) {
        illegal := False
        rdata := mie
        when(io.cmd.wen) { mie := io.cmd.wdata.asBits }
      }
      is(CSRs.Mtvec) {
        illegal := False
        rdata := mtvec
        when(io.cmd.wen) { mtvec := io.cmd.wdata.asBits }
      }
      is(CSRs.Mcounteren) {
        illegal := False
        rdata := mcounteren
        when(io.cmd.wen) { mcounteren := io.cmd.wdata.asBits }
      }

      // ---- Machine Trap Handling (read/write) ----
      is(CSRs.Mscratch) {
        illegal := False
        rdata := mscratch
        when(io.cmd.wen) { mscratch := io.cmd.wdata.asBits }
      }
      is(CSRs.Mepc) {
        illegal := False
        rdata := mepc
        when(io.cmd.wen) { mepc := io.cmd.wdata.asBits }
      }
      is(CSRs.Mcause) {
        illegal := False
        rdata := mcause
        when(io.cmd.wen) { mcause := io.cmd.wdata.asBits }
      }
      is(CSRs.Mtval) {
        illegal := False
        rdata := mtval
        when(io.cmd.wen) { mtval := io.cmd.wdata.asBits }
      }
      is(CSRs.Mip) {
        illegal := False
        rdata := mip
        when(io.cmd.wen) { mip := io.cmd.wdata.asBits }
      }
    }
  }

  // Pipeline the response: rdata and illegal are combinational in cycle of cmd.valid
  // For the output, register them so rsp is available next cycle
  val rdataReg   = RegNextWhen(rdata, io.cmd.valid) init B(0, 64 bits)
  val illegalReg = RegNextWhen(illegal, io.cmd.valid) init False
  val validReg   = RegNext(io.cmd.valid) init False

  io.rsp.rdata   := rdataReg
  io.rsp.illegal := illegalReg
  io.rsp.valid   := validReg
}

/**
 * Top-level wrapper for standalone Verilog generation.
 */
class CSRTop extends Component {
  val io = new Bundle {
    val clk   = in Bool()
    val reset = in Bool()
    val cmd_valid = in Bool()
    val cmd_addr  = in UInt(12 bits)
    val cmd_wdata = in Bits(64 bits)
    val cmd_wen   = in Bool()
    val rsp_rdata   = out Bits(64 bits)
    val rsp_valid   = out Bool()
    val rsp_illegal = out Bool()
  }

  val cd = ClockDomain(clock = io.clk, reset = io.reset)
  val area = new ClockingArea(cd) {
    val csr = new CSR
    csr.io.cmd.valid := io.cmd_valid
    csr.io.cmd.addr  := io.cmd_addr
    csr.io.cmd.wdata := io.cmd_wdata
    csr.io.cmd.wen   := io.cmd_wen
    io.rsp_rdata   := csr.io.rsp.rdata
    io.rsp_valid   := csr.io.rsp.valid
    io.rsp_illegal := csr.io.rsp.illegal
  }
}

/**
 * Generates standalone Verilog/SystemVerilog for the CSR module.
 *
 * Usage:
 *   mill -i changbaiV1.spinal.runMain v1.csr.GenCSR
 *
 * Output: changbai/rtl/CSRTop.sv
 */
object GenCSR {
  def main(args: Array[String]): Unit = {
    SpinalConfig(
      mode = SystemVerilog,
      targetDirectory = "rtl",
      genLineComments = true,
      oneFilePerComponent = true,
      withTimescale = false,
      printFilelist = false
    ).generate {
      new CSRTop
    }

    println("Generated rtl/CSRTop.sv")
  }
}
