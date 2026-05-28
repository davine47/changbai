// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1

import spinal.core._
import spinal.lib._

/**
 * ALU operation encoding.
 *
 * Encoding follows RISC-V funct3 convention where possible:
 *   op[2:0] = funct3
 *   op[3]   = op32 (1 = 32-bit operation with sign-extension to 64-bit)
 *   op[4]   = alt   (for funct3=000: 0=ADD/ADDI, 1=SUB; for funct3=101: 0=SRL/SRLI, 1=SRA/SRAI)
 *
 *   Special opcodes: LUI, AUIPC, JAL, JALR (use op=10..12, i.e., 0b01010..0b01100)
 */
object AluOp {
  val width = 5

  // funct3=000: ADD/SUB/MUL
  def ADD   = B"00000"  // rd = rs1 + rs2
  def SUB   = B"10000"  // rd = rs1 - rs2
  def ADDW  = B"01000"  // rd = sext32(rs1[31:0] + rs2[31:0])
  def SUBW  = B"11000"  // rd = sext32(rs1[31:0] - rs2[31:0])

  // funct3=001: SLL
  def SLL   = B"00001"  // rd = rs1 << rs2[5:0]
  def SLLW  = B"01001"  // rd = sext32(rs1[31:0] << rs2[4:0])

  // funct3=010: SLT
  def SLT   = B"00010"  // rd = (signed(rs1) < signed(rs2)) ? 1 : 0

  // funct3=011: SLTU
  def SLTU  = B"00011"  // rd = (rs1 < rs2) ? 1 : 0
  def BGT   = B"00100"  // rd = (rs1 > rs2) ? 1 : 0 (signed, rs1 > rs2 ≡ rs2 < rs1)
  def BGTU  = B"00101"  // rd = (rs1 > rs2) ? 1 : 0 (unsigned)

  // funct3=100: XOR
  def XOR   = B"00100"  // rd = rs1 ^ rs2

  // funct3=101: SRL/SRA
  def SRL   = B"00101"  // rd = rs1 >> rs2[5:0] (logical)
  def SRA   = B"10101"  // rd = rs1 >>> rs2[5:0] (arithmetic)
  def SRLW  = B"01101"  // rd = sext32(rs1[31:0] >> rs2[4:0])
  def SRAW  = B"11101"  // rd = sext32(rs1[31:0] >>> rs2[4:0])

  // funct3=110: OR
  def OR    = B"00110"  // rd = rs1 | rs2

  // funct3=111: AND
  def AND   = B"00111"  // rd = rs1 & rs2

  // Auxiliary ops for immediate/U-type instructions (use unused op slots)
  def LUI   = B"01010"  // rd = imm (LUI: upper immediate passed via src1)
  def AUIPC = B"01011"  // rd = rs1 + rs2 (AUIPC: rs1=PC, rs2=U-immediate)
  def JAL   = B"01100"  // rd = rs1 + 4 (PC+4 for JAL/JALR return address)

  // Helper: extract funct3 from op
  def funct3(op: Bits): UInt = op(2 downto 0).asUInt
  def isWord(op: Bits): Bool = op(3)
  def isAlt(op: Bits): Bool  = op(4)
}

/**
 * ALU configuration.
 *
 * @param xlen   data width (32 for RV32, 64 for RV64)
 */
case class AluConfig(
    xlen: Int = 64
)

/**
 * RISC-V ALU — arithmetic, logical, and shift operations.
 *
 * Implements RV32I/RV64I basic operations:
 *   - ADD, SUB, ADDW, SUBW
 *   - SLL, SRL, SRA, SLLW, SRLW, SRAW
 *   - SLT, SLTU
 *   - AND, OR, XOR
 *   - LUI, AUIPC, JAL, JALR (pass-through with PC)
 *
 * Ports:
 *   io.src0    — operand 0 (rs1)
 *   io.src1    — operand 1 (rs2 or immediate)
 *   io.aluOp   — operation select (5-bit, see AluOp)
 *   io.result  — computation result
 */
class Alu(config: AluConfig) extends Component {
  import config._

  val io = new Bundle {
    val src0   = in Bits(xlen bits)
    val src1   = in Bits(xlen bits)
    val aluOp  = in Bits(AluOp.width bits)
    val result = out Bits(xlen bits)
  }

  import AluOp._

  val op     = io.aluOp
  val funct3 = op(2 downto 0).asUInt
  val is32   = op(3)
  val isAlt  = op(4)

  val src0 = io.src0
  val src1 = io.src1

  // =========================================================================
  // 32-bit operands (for W-suffix operations)
  // =========================================================================
  val src0w = src0(31 downto 0).asUInt
  val src1w = src1(31 downto 0).asUInt

  // =========================================================================
  // Shift amounts
  // =========================================================================
  val shamt   = src1(5 downto 0).asUInt   // 6-bit for RV64
  val shamtW  = src1(4 downto 0).asUInt   // 5-bit for RV64 W-suffix

  // =========================================================================
  // =========================================================================
  // ADD/SUB (funct3=000)
  // =========================================================================
  val adderOut = src0.asUInt + src1.asUInt
  val subberOut = src0.asUInt - src1.asUInt
  // 32-bit operations: resize to 32 bits to match RV64 W-suffix semantics
  val adderWOut = (src0w + src1w).resize(32 bits)
  val subberWOut = (src0w - src1w).resize(32 bits)

  // =========================================================================
  // Shift operations
  // =========================================================================
  // SLL (funct3=001)
  val sllOut = src0 |<< shamt

  // SRL (funct3=101, isAlt=0)
  val srlOut = src0 |>> shamt

  // SRA (funct3=101, isAlt=1)
  val sraOut = src0.asSInt |>> shamt

  // SLLW (funct3=001, is32=1)
  val sllwOut = (src0w |<< shamtW).resize(32 bits)

  // SRLW (funct3=101, isAlt=0, is32=1)
  val srlwOut = (src0w |>> shamtW).resize(32 bits)

  // SRAW (funct3=101, is32=1, isAlt=1)
  val srawOut = (src0w.asSInt |>> shamtW).resize(32 bits)

  // =========================================================================
  // Set-less-than (funct3=010, 011)
  // =========================================================================
  val sltOut  = src0.asSInt < src1.asSInt   // signed
  val sltuOut = src0.asUInt < src1.asUInt   // unsigned
  val bgtOut  = src0.asSInt > src1.asSInt   // signed greater-than
  val bgtuOut = src0.asUInt > src1.asUInt   // unsigned greater-than

  // =========================================================================
  // Logic operations (funct3=100, 110, 111)
  // =========================================================================
  val xorOut = src0 ^ src1
  val orOut  = src0 | src1
  val andOut = src0 & src1

  // =========================================================================
  // Result selection via MuxOH based on funct3 + is32 + isAlt
  // =========================================================================

  // We decode the full 5-bit aluOp to select the result
  val result = Bits(xlen bits)

  // Build a mapping table: for each possible 5-bit opcode, select the result
  // We use a Vec of all possible results indexed by aluOp
  val resultMap = Vec.fill(32)(B(0, xlen bits))

  // W-suffix results: sign-extend 32-bit result to 64-bit
  // Use Mux for proper sign-bit replication (B(bool, N) only sets LSB!)
  def sext32To64(hi: Bool, lo: Bits): Bits = {
    Mux(hi, B"32'hFFFFFFFF", B"32'h0") ## lo.asBits
  }
  resultMap(ADDW.asUInt)   := sext32To64(adderWOut(31), adderWOut.asBits)
  resultMap(SUBW.asUInt)   := sext32To64(subberWOut(31), subberWOut.asBits)
  resultMap(SLLW.asUInt)   := sext32To64(sllwOut(31), sllwOut.asBits)
  resultMap(SRLW.asUInt)   := sext32To64(srlwOut(31), srlwOut.asBits)
  resultMap(SRAW.asUInt)   := sext32To64(srawOut(31), srawOut.asBits)

  // Non-W results
  resultMap(ADD.asUInt)    := adderOut.asBits
  resultMap(SUB.asUInt)    := subberOut.asBits
  resultMap(SLL.asUInt)    := sllOut.asBits
  resultMap(SLT.asUInt)    := Mux(sltOut, B(1, xlen bits), B(0, xlen bits))
  resultMap(SLTU.asUInt)   := Mux(sltuOut, B(1, xlen bits), B(0, xlen bits))
  resultMap(BGT.asUInt)    := Mux(bgtOut, B(1, xlen bits), B(0, xlen bits))
  resultMap(BGTU.asUInt)   := Mux(bgtuOut, B(1, xlen bits), B(0, xlen bits))
  resultMap(XOR.asUInt)    := xorOut
  resultMap(SRL.asUInt)    := srlOut.asBits
  resultMap(SRA.asUInt)    := sraOut.asBits
  resultMap(OR.asUInt)     := orOut
  resultMap(AND.asUInt)    := andOut
  resultMap(LUI.asUInt)    := src1
  resultMap(AUIPC.asUInt)  := adderOut.asBits
  resultMap(JAL.asUInt)    := adderOut.asBits

  result := resultMap(op.asUInt)

  io.result := result
}

// =============================================================================
// Top-level wrapper + Generator
// =============================================================================

/**
 * Top-level wrapper for standalone Verilog generation.
 */
class AluTop(config: AluConfig = AluConfig()) extends Component {
  val io = new Bundle {
    val clk    = in Bool()
    val reset  = in Bool()
    val src0   = in Bits(config.xlen bits)
    val src1   = in Bits(config.xlen bits)
    val aluOp  = in Bits(AluOp.width bits)
    val result = out Bits(config.xlen bits)
  }

  val cd = ClockDomain(clock = io.clk, reset = io.reset)
  val area = new ClockingArea(cd) {
    val alu = new Alu(config)
    alu.io.src0  := io.src0
    alu.io.src1  := io.src1
    alu.io.aluOp := io.aluOp
    io.result    := alu.io.result
  }
}

/**
 * Generates standalone Verilog/SystemVerilog for the ALU module.
 *
 * Usage:
 *   mill -i changbaiV1.spinal.runMain v1.alu.GenAlu
 *
 * Output: changbai/rtl/AluTop.sv
 */
object GenAlu {
  def main(args: Array[String]): Unit = {
    val xlen = if (args.length >= 1) args(0).toInt else 64
    val config = AluConfig(xlen = xlen)

    SpinalConfig(
      mode = SystemVerilog,
      targetDirectory = "rtl",
      genLineComments = true,
      oneFilePerComponent = true,
      withTimescale = false,
      printFilelist = false
    ).generate {
      new AluTop(config)
    }

    println(s"Generated rtl/AluTop.sv (XLEN=$xlen)")
  }
}
