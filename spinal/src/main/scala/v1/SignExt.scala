// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1

import spinal.core._

// =============================================================================
// immExtType encoding
// =============================================================================
object ImmExtType {
  val width = 3

  def S  = spinal.core.U(1, width bits)  // S-type store offset (12-bit signed)
  def SB = spinal.core.U(2, width bits)  // B-type branch offset (13-bit signed, bit0=0)
  def U  = spinal.core.U(3, width bits)  // U-type upper immediate (32-bit unsigned)
  def UJ = spinal.core.U(4, width bits)  // J-type jump offset (21-bit signed, bit0=0)
  def I  = spinal.core.U(5, width bits)  // I-type immediate (12-bit signed)
  def Z  = spinal.core.U(6, width bits)  // Z-type rs1 field (5-bit unsigned)
}

// =============================================================================
// SignExt — RISC-V instruction immediate sign extension
//
// Extract specified-type immediate from 32-bit instruction and
// sign-extend to xlen width.
// Immediate field extraction logic derived from VexRiscv Riscv.scala IMM.
//
// immExtType encoding:
//   001 = S   (S-type store offset,    12-bit signed → sext to xlen)
//   010 = SB  (B-type branch offset,   13-bit signed, bit0=0 → sext to xlen)
//   011 = U   (U-type upper immediate, 32-bit unsigned → zero-ext to xlen)
//   100 = UJ  (J-type jump offset,     21-bit signed, bit0=0 → sext to xlen)
//   101 = I   (I-type immediate,       12-bit signed → sext to xlen)
//   110 = Z   (shamt,                   5-bit unsigned → zero-ext to xlen)
//   000 = reserved (output = 0)
// =============================================================================

class SignExt(xlen: Int = 64) extends Component {
  val io = new Bundle {
    val instruction = in  Bits(32 bits)
    val immExtType  = in  UInt(3 bits)
    val immediate   = out Bits(xlen bits)
  }

  // =========================================================================
  // Immediate field extraction (VexRiscv IMM logic)
  // =========================================================================
  val i_imm = io.instruction(31 downto 20)                                      // I-type: inst[31:20]
  val s_imm = io.instruction(31 downto 25) ## io.instruction(11 downto 7)      // S-type: inst[31:25] ## inst[11:7]
  val b_imm = io.instruction(31) ## io.instruction(7) ##
              io.instruction(30 downto 25) ## io.instruction(11 downto 8)       // B-type: inst[31|7|30:25|11:8]
  val u_imm = io.instruction(31 downto 12) ## U"x000"                          // U-type: inst[31:12] << 12
  val j_imm = io.instruction(31) ## io.instruction(19 downto 12) ##
              io.instruction(20) ## io.instruction(30 downto 21)                // J-type: inst[31|19:12|20|30:21]
  val z_imm = io.instruction(19 downto 15)                                      // Z-type: shamt[4:0]

  // =========================================================================
  // Sign extension to xlen
  // =========================================================================
  // I-type: 12-bit → sext
  val i_sext  = B((xlen - 13 downto 0) -> i_imm(11)) ## i_imm
  // S-type: 12-bit → sext
  val s_sext  = B((xlen - 13 downto 0) -> s_imm(11)) ## s_imm
  // B-type: 13-bit (bit0=0) → sext: b_imm is 12-bit MSB part
  val b_sext  = B((xlen - 14 downto 0) -> b_imm(11)) ## b_imm ## False
  // U-type: 32-bit → zero-ext
  val u_ext   = (if (xlen > 32) B(0, (xlen - 32) bits) ## u_imm else u_imm)
  // J-type: 21-bit (bit0=0) → sext: j_imm is 20-bit MSB part
  val j_sext  = B((xlen - 22 downto 0) -> j_imm(19)) ## j_imm ## False
  // Z-type: 5-bit → zero-ext
  val z_ext   = B(0, (xlen - 5) bits) ## z_imm

  // =========================================================================
  // Output selection
  // =========================================================================
  // Use switch for mux, default=0 corresponds to encoding 000
  val result = Bits(xlen bits)
  result := 0
  switch(io.immExtType) {
    is(1) { result := s_sext }
    is(2) { result := b_sext }
    is(3) { result := u_ext }
    is(4) { result := j_sext }
    is(5) { result := i_sext }
    is(6) { result := z_ext }
  }
  io.immediate := result
}

// =============================================================================
// Top-level wrapper for cocotb verification
// =============================================================================

class SignExtTop(xlen: Int = 64) extends Component {
  val io = new Bundle {
    val clk         = in  Bool()
    val reset       = in  Bool()
    val instruction = in  Bits(32 bits)
    val immExtType  = in  UInt(3 bits)
    val immediate   = out Bits(xlen bits)
  }

  val coreClockDomain = ClockDomain(clock = io.clk, reset = io.reset)
  val area = new ClockingArea(coreClockDomain) {
    val signExt = new SignExt(xlen)
    signExt.io.instruction := io.instruction
    signExt.io.immExtType  := io.immExtType
    io.immediate           := signExt.io.immediate
  }
}

// =============================================================================
// Generator
// =============================================================================

object GenSignExt {
  def main(args: Array[String]): Unit = {
    val xlen = if (args.length >= 1) args(0).toInt else 64

    SpinalConfig(
      mode = SystemVerilog,
      targetDirectory = "rtl",
      genLineComments = true,
      oneFilePerComponent = true,
      withTimescale = false,
      printFilelist = false
    ).generate {
      new SignExtTop(xlen)
    }
    println(s"Generated rtl/SignExtTop.sv (XLEN=$xlen)")
  }
}
