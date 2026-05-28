// This file is AI[deepseek-v4-pro, xhigh]-generated and manually verified.
package v1

import spinal.core._
import spinal.lib.logic._
import v1.utils.{AbstractDecodeSigs, DecodeConst}

// =============================================================================
// Operation encodings (per scala-decode.md blueprint)
// =============================================================================

object MemOp {
  val width = 5
  def LB = B"00000"; def LH = B"00001"; def LW = B"00010"; def LD = B"00011"
  def SB = B"00100"; def SH = B"00101"; def SW = B"00110"; def SD = B"00111"
}

object MemResOp {
  val width = 5
  def nop = B"00000"; def sExt = B"00001"; def uExt = B"00010"
}

object CsrOp {
  val width = 5
  def nop = B"00000"; def rw  = B"00001"; def rs  = B"00010"
  def rc  = B"00011"; def rwi = B"00100"; def rsi = B"00101"; def rci = B"00110"
}

object ImmExt {
  def S = B"001"; def SB = B"010"; def U = B"011"
  def UJ= B"100"; def I = B"101"; def Z = B"110"
}

// =============================================================================
// MaskedLiteral helpers for the decode table
// =============================================================================

object SDVal {
  // 1-bit
  val N = M"0"
  val Y = M"1"

  // 3-bit (immExtType)
  val I_N  = M"000"
  val I_S  = M"001"
  val I_SB = M"010"
  val I_U  = M"011"
  val I_UJ = M"100"
  val I_I  = M"101"
  val I_Z  = M"110"

  // 5-bit (aluOp)
  val aluOp_ADD   = M"00000"
  val aluOp_SLL   = M"00001"
  val aluOp_SLT   = M"00010"
  val aluOp_SLTU  = M"00011"
  val aluOp_BGT   = M"00100"  // same encoding as XOR
  val aluOp_BGTU  = M"00101"  // same encoding as SRL
  val aluOp_XOR   = M"00100"
  val aluOp_SRL   = M"00101"
  val aluOp_SRA   = M"10101"
  val aluOp_OR    = M"00110"
  val aluOp_AND   = M"00111"
  val aluOp_ADDW  = M"01000"
  val aluOp_SLLW  = M"01001"
  val aluOp_LUI   = M"01010"
  val aluOp_AUIPC = M"01011"
  val aluOp_SUB   = M"10000"
  val aluOp_SUBW  = M"11000"
  val aluOp_SRLW  = M"01101"
  val aluOp_SRAW  = M"11101"

  // 5-bit (memOp)
  val M_LB = M"00000"; val M_LH = M"00001"; val M_LW = M"00010"; val M_LD = M"00011"
  val M_SB = M"00100"; val M_SH = M"00101"; val M_SW = M"00110"; val M_SD = M"00111"
  val M_X  = M"00000"

  // 5-bit (memResOp)
  val R_nop  = M"00000"; val R_sExt = M"00001"; val R_uExt = M"00010"

  // 5-bit (csrOp)
  val C_nop = M"00000"; val C_rw  = M"00001"; val C_rs  = M"00010"
  val C_rc  = M"00011";  val C_rwi = M"00100"; val C_rsi = M"00101"; val C_rci = M"00110"
}

// =============================================================================
// ScalarDecodeBundle — 19 fields, per blueprint
//   legal(1) branch(1) jal(1) jalr(1) rrf1(1) rrf2(1) wrf1(1) useALU(1) aluOp(5)
//   useMem(1) memOp(5) memResOp(5) useCsr(1) csrOp(5)
//   needImmExt(1) immExtType(3) fence(1) fenceI(1) amo(1)
// Total: 37 bits
// =============================================================================

class ScalarDecodeBundle extends Bundle {
  val legal      = Bool()       // 0
  val branch     = Bool()       // 1
  val jal        = Bool()       // 2
  val jalr       = Bool()       // 3
  val rrf1       = Bool()       // 4
  val rrf2       = Bool()       // 5
  val wrf1       = Bool()       // 6
  val useALU     = Bool()       // 7
  val aluOp      = Bits(5 bits) // 12:8
  val useMem     = Bool()       // 13
  val memOp      = Bits(5 bits) // 18:14
  val memResOp   = Bits(5 bits) // 23:19
  val useCsr     = Bool()       // 24
  val csrOp      = Bits(5 bits) // 29:25
  val needImmExt = Bool()       // 30
  val immExtType = Bits(3 bits) // 33:31
  val fence      = Bool()       // 34
  val fenceI     = Bool()       // 35
  val amo        = Bool()       // 36
}

// =============================================================================
// ScalarDecodeTableConst — decode table (per scala-decode.md blueprint)
//
// Field order (except legal which is derived from pattern matching):
//   List(branch, jal, jalr, rrf1, rrf2, wrf1, useALU, aluOp,
//        useMem, memOp, memResOp, useCsr, csrOp,
//        needImmExt, immExtType, fence, fenceI, amo)
// =============================================================================

class ScalarDecodeTableConst extends DecodeConst {
  import SDVal._
  import Instructions._

  // FENCE.I pattern (funct3=001, same opcode as FENCE)
  private val FENCEI = M"-----------------001-----0001111"

  // AMO catch-all pattern (any funct3 within opcode 0101111)
  private val AMOANY = M"-------------------------0101111"

  // CSR instruction patterns (not in Instructions.scala)
  private val CSRW  = M"-----------------001-----1110011"
  private val CSRS  = M"-----------------010-----1110011"
  private val CSRC  = M"-----------------011-----1110011"
  private val CSRWI = M"-----------------101-----1110011"
  private val CSRSI = M"-----------------110-----1110011"
  private val CSRCI = M"-----------------111-----1110011"

  // SYSTEM funct3=000 catch-all (MRET/WFI etc.)
  private val SYS000 = M"-----------------000-----1110011"

  override val table: Array[(MaskedLiteral, List[MaskedLiteral])] = Array(
    // ---- ALU R-type ----
    ADD   -> List(N,N,N,Y,Y,Y,Y, aluOp_ADD,  N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    SUB   -> List(N,N,N,Y,Y,Y,Y, aluOp_SUB,  N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    SLL   -> List(N,N,N,Y,Y,Y,Y, aluOp_SLL,  N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    SLT   -> List(N,N,N,Y,Y,Y,Y, aluOp_SLT,  N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    SLTU  -> List(N,N,N,Y,Y,Y,Y, aluOp_SLTU, N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    XOR   -> List(N,N,N,Y,Y,Y,Y, aluOp_XOR,  N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    SRL   -> List(N,N,N,Y,Y,Y,Y, aluOp_SRL,  N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    SRA   -> List(N,N,N,Y,Y,Y,Y, aluOp_SRA,  N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    OR    -> List(N,N,N,Y,Y,Y,Y, aluOp_OR,   N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    AND   -> List(N,N,N,Y,Y,Y,Y, aluOp_AND,  N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),

    // ---- ALU W-suffix R-type ----
    ADDW  -> List(N,N,N,Y,Y,Y,Y, aluOp_ADDW, N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    SUBW  -> List(N,N,N,Y,Y,Y,Y, aluOp_SUBW, N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    SLLW  -> List(N,N,N,Y,Y,Y,Y, aluOp_SLLW, N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    SRLW  -> List(N,N,N,Y,Y,Y,Y, aluOp_SRLW, N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    SRAW  -> List(N,N,N,Y,Y,Y,Y, aluOp_SRAW, N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),

    // ---- ALU I-type ----
    ADDI  -> List(N,N,N,Y,N,Y,Y, aluOp_ADD,  N,M_X,R_nop, N,C_nop, Y,I_I,  N,N,N),
    SLLI  -> List(N,N,N,Y,N,Y,Y, aluOp_SLL,  N,M_X,R_nop, N,C_nop, Y,I_I,  N,N,N),
    SLTI  -> List(N,N,N,Y,N,Y,Y, aluOp_SLT,  N,M_X,R_nop, N,C_nop, Y,I_I,  N,N,N),
    SLTIU -> List(N,N,N,Y,N,Y,Y, aluOp_SLTU, N,M_X,R_nop, N,C_nop, Y,I_I,  N,N,N),
    XORI  -> List(N,N,N,Y,N,Y,Y, aluOp_XOR,  N,M_X,R_nop, N,C_nop, Y,I_I,  N,N,N),
    SRLI  -> List(N,N,N,Y,N,Y,Y, aluOp_SRL,  N,M_X,R_nop, N,C_nop, Y,I_I,  N,N,N),
    SRAI  -> List(N,N,N,Y,N,Y,Y, aluOp_SRA,  N,M_X,R_nop, N,C_nop, Y,I_I,  N,N,N),
    ORI   -> List(N,N,N,Y,N,Y,Y, aluOp_OR,   N,M_X,R_nop, N,C_nop, Y,I_I,  N,N,N),
    ANDI  -> List(N,N,N,Y,N,Y,Y, aluOp_AND,  N,M_X,R_nop, N,C_nop, Y,I_I,  N,N,N),

    // ---- ALU I-type W-suffix (OP-IMM-32) ----
    ADDIW -> List(N,N,N,Y,N,Y,Y, aluOp_ADDW, N,M_X,R_nop, N,C_nop, Y,I_I,  N,N,N),
    SLLIW -> List(N,N,N,Y,N,Y,Y, aluOp_SLLW, N,M_X,R_nop, N,C_nop, Y,I_I,  N,N,N),
    SRLIW -> List(N,N,N,Y,N,Y,Y, aluOp_SRLW, N,M_X,R_nop, N,C_nop, Y,I_I,  N,N,N),
    SRAIW -> List(N,N,N,Y,N,Y,Y, aluOp_SRAW, N,M_X,R_nop, N,C_nop, Y,I_I,  N,N,N),

    // ---- LUI / AUIPC ----
    LUI   -> List(N,N,N,Y,N,Y,Y, aluOp_LUI,   N,M_X,R_nop, N,C_nop, Y,I_U,  N,N,N),
    AUIPC -> List(N,N,N,Y,N,Y,Y, aluOp_AUIPC, N,M_X,R_nop, N,C_nop, Y,I_U,  N,N,N),

    // ---- LOAD ----
    LB    -> List(N,N,N,Y,N,Y,N, aluOp_ADD,  Y,M_LB,R_sExt, N,C_nop, Y,I_I,  N,N,N),
    LH    -> List(N,N,N,Y,N,Y,N, aluOp_ADD,  Y,M_LH,R_sExt, N,C_nop, Y,I_I,  N,N,N),
    LW    -> List(N,N,N,Y,N,Y,N, aluOp_ADD,  Y,M_LW,R_sExt, N,C_nop, Y,I_I,  N,N,N),
    LD    -> List(N,N,N,Y,N,Y,N, aluOp_ADD,  Y,M_LW,R_nop,  N,C_nop, Y,I_I,  N,N,N),
    LBU   -> List(N,N,N,Y,N,Y,N, aluOp_ADD,  Y,M_LB,R_uExt, N,C_nop, Y,I_I,  N,N,N),
    LHU   -> List(N,N,N,Y,N,Y,N, aluOp_ADD,  Y,M_LH,R_uExt, N,C_nop, Y,I_I,  N,N,N),
    LWU   -> List(N,N,N,Y,N,Y,N, aluOp_ADD,  Y,M_LW,R_uExt, N,C_nop, Y,I_I,  N,N,N),

    // ---- STORE ----
    SB    -> List(N,N,N,Y,N,N,N, aluOp_ADD,  Y,M_SB,R_nop,  N,C_nop, Y,I_S,  N,N,N),
    SH    -> List(N,N,N,Y,N,N,N, aluOp_ADD,  Y,M_SH,R_nop,  N,C_nop, Y,I_S,  N,N,N),
    SW    -> List(N,N,N,Y,N,N,N, aluOp_ADD,  Y,M_SW,R_nop,  N,C_nop, Y,I_S,  N,N,N),
    SD    -> List(N,N,N,Y,N,N,N, aluOp_ADD,  Y,M_SD,R_nop,  N,C_nop, Y,I_S,  N,N,N),

    // ---- BRANCH ----
    BEQ   -> List(Y,N,N,Y,Y,N,N, aluOp_ADD,   N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    BNE   -> List(Y,N,N,Y,Y,N,N, aluOp_ADD,   N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    BLT   -> List(Y,N,N,Y,Y,N,Y, aluOp_SLT,  N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    BGE   -> List(Y,N,N,Y,Y,N,Y, aluOp_BGT,  N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    BLTU  -> List(Y,N,N,Y,Y,N,Y, aluOp_SLTU, N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    BGEU  -> List(Y,N,N,Y,Y,N,Y, aluOp_BGTU, N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),

    // ---- JAL / JALR ----
    JAL   -> List(N,Y,N,N,N,Y,Y, aluOp_ADD,  N,M_X,R_nop, N,C_nop, Y,I_UJ, N,N,N),
    JALR  -> List(N,N,Y,N,N,Y,Y, aluOp_ADD,  N,M_X,R_nop, N,C_nop, Y,I_I,  N,N,N),

    // ---- CSR ----
    CSRW  -> List(N,N,N,Y,N,N,N, aluOp_ADD,  N,M_X,R_nop, Y,C_rw,  N,I_N,  N,N,N),
    CSRS  -> List(N,N,N,Y,N,N,N, aluOp_ADD,  N,M_X,R_nop, Y,C_rs,  N,I_N,  N,N,N),
    CSRC  -> List(N,N,N,Y,N,N,N, aluOp_ADD,  N,M_X,R_nop, Y,C_rc,  N,I_N,  N,N,N),
    CSRWI -> List(N,N,N,Y,N,N,N, aluOp_ADD,  N,M_X,R_nop, Y,C_rwi, N,I_N,  N,N,N),
    CSRSI -> List(N,N,N,Y,N,N,N, aluOp_ADD,  N,M_X,R_nop, Y,C_rsi, N,I_N,  N,N,N),
    CSRCI -> List(N,N,N,Y,N,N,N, aluOp_ADD,  N,M_X,R_nop, Y,C_rci, N,I_N,  N,N,N),

    // ---- FENCE / FENCE.I ----
    FENCE  -> List(N,N,N,N,N,N,N, aluOp_ADD,  N,M_X,R_nop, N,C_nop, N,I_N,  Y,N,N),
    FENCEI -> List(N,N,N,N,N,N,N, aluOp_ADD,  N,M_X,R_nop, N,C_nop, N,I_N,  N,Y,N),

    // ---- AMO (catch-all for opcode 0101111) ----
    AMOANY -> List(N,N,N,Y,Y,Y,N, aluOp_ADD,  Y,M_LW,R_nop,  N,C_nop, N,I_N,  N,N,Y),

    // ---- SYSTEM (ECALL/EBREAK/MRET/WFI) - catch-all funct3=000 ----
    ECALL  -> List(N,N,N,N,N,N,N, aluOp_ADD,  N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    EBREAK -> List(N,N,N,N,N,N,N, aluOp_ADD,  N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N),
    SYS000 -> List(N,N,N,N,N,N,N, aluOp_ADD,  N,M_X,R_nop, N,C_nop, N,I_N,  N,N,N)
  )
}

// =============================================================================
// ScalarDecodeSigs — decode signal generation (per VectorDecodeDemo pattern)
// =============================================================================

class ScalarDecodeSigs(needs: DecodeConst, coverAll: Seq[Masked], spec: DecodingSpec[Bits])
  extends AbstractDecodeSigs[Bits](needs, coverAll, spec) with Area {
  import SDVal._

  override val sigs = new ScalarDecodeBundle

  // Default: all zeros for 18 fields (legal is derived from pattern matching)
  // List order: branch, jal, jalr, rrf1, rrf2, wrf1, useALU, aluOp,
  //             useMem, memOp, memResOp, useCsr, csrOp,
  //             needImmExt, immExtType, fence, fenceI, amo
  override val default: List[MaskedLiteral] = List(
    N,N,N,N,N,N,N, aluOp_ADD,
    N,M_X,R_nop, N,C_nop,
    N,I_N,
    N,N,N
  )

  // Override decode to invert legal -> ill (ill=1 indicates illegal)
  override def decode(in: Bits): Bits = {
    needs.table.foreach(i => spec.addNeeds(Masked(i._1), Masked(needs.unwrap(i._2))))
    val legal = Symplify.logicOf(in,
      SymplifyBit.getPrimeImplicantsByTrueAndDontCare(coverAll, Nil, in.getBitsWidth))
    spec.setDefault(Masked(needs.unwrap(default)))
    val decodeRes = spec.build(in, coverAll).asBits
    // legal=1 means valid instruction, legal=0 means illegal (other fields are don't-care)
    val decodeResWithLegal = Cat(decodeRes, legal.asBits)
    sigs.assignFromBits(decodeResWithLegal)
    sigs.asBits
  }
}

// =============================================================================
// ScalarDecodeSigs companion (factory method, per VectorDecodeDemo pattern)
// =============================================================================

object ScalarDecodeSigs {
  def apply(needs: DecodeConst): ScalarDecodeSigs = {
    val coverAll = needs.table.map(i => Masked(i._1)).toSeq
    val spec = new DecodingSpec(HardType(Bits(36 bits)))
    new ScalarDecodeSigs(needs, coverAll, spec)
  }
}

// =============================================================================
// ScalarDecode — RV64IMC combinational decode (table-driven via Decode.scala)
// =============================================================================

class ScalarDecode extends Component {
  val io = new Bundle {
    val inst    = in  Bits(32 bits)
    val instIll = in  Bool()  // RVC illegal: force legal=0 when set
    val decode  = out(new ScalarDecodeBundle)
  }

  val decodeArea = ScalarDecodeSigs(new ScalarDecodeTableConst)
  io.decode.assignFromBits(decodeArea.decode(io.inst))

  // Override legal: RVC illegal instructions are not legal
  when(io.instIll) {
    io.decode.legal := False
  }
}

// =============================================================================
// Top-level wrapper for Verilator verification
// =============================================================================

class ScalarDecodeTop extends Component {
  val io = new Bundle {
    val clk    = in  Bool()
    val reset  = in  Bool()
    val inst   = in  Bits(32 bits)
    val decode = out(new ScalarDecodeBundle)
  }

  val coreClockDomain = ClockDomain(clock = io.clk, reset = io.reset)
  val area = new ClockingArea(coreClockDomain) {
    val decoder = new ScalarDecode
    decoder.io.inst    := io.inst
    decoder.io.instIll := False  // standalone: no RVC context
    io.decode := decoder.io.decode
  }
}

// =============================================================================
// Generator
// =============================================================================

object GenScalarDecode {
  def main(args: Array[String]): Unit = {
    SpinalConfig(
      mode = SystemVerilog,
      targetDirectory = "rtl",
      genLineComments = true,
      oneFilePerComponent = true,
      withTimescale = false,
      printFilelist = false
    ).generate {
      new ScalarDecodeTop
    }
    println("Generated rtl/ScalarDecodeTop.sv")
  }
}
