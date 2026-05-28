// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1

import spinal.core._
import spinal.lib._

case class ExpandedInstruction() extends Bundle {
  val bits = Bits(32 bits)
  val rd   = UInt(5 bits)
  val rs1  = UInt(5 bits)
  val rs2  = UInt(5 bits)
  val rs3  = UInt(5 bits)
}

// =============================================================================
// RVCExpander — 16-bit compressed → 32-bit instruction, pure combinational
// =============================================================================

class RVCExpander extends Component {
  val io = new Bundle {
    val instIn  = in  Bits(16 bits)
    val instOut = out(ExpandedInstruction())
    val rvc     = out Bool()
    val ill     = out Bool()
  }

  val x = io.instIn

  def rs1p  = B"01" ## x(9 downto 7)
  def rs2p  = B"01" ## x(4 downto 2)
  def rs2u  = x(6 downto 2).asUInt
  def rd    = x(11 downto 7).asUInt
  def x0    = U(0, 5 bits)
  def ra    = U(1, 5 bits)
  def sp    = U(2, 5 bits)

  io.rvc := x(1 downto 0) =/= B"11"

  val opIdx = (x(1 downto 0) ## x(15 downto 13)).asUInt

  // ---- Pre-compute all instruction variants as combinational signals ----

  // C.ADDI4SPN (opIdx=0)
  val nzuimm_0 = B"00" ## x(10 downto 7) ## x(12 downto 11) ## x(5) ## x(6) ## B"00"
  val adi4spn_bits = nzuimm_0 ## sp.asBits ## B"000" ## rs2p ## B"0010011"
  val adi4spn_valid = x(12 downto 5).orR

  // C.LW (opIdx=2)
  val lw_off = B"00000" ## x(5) ## x(12 downto 10) ## x(6) ## B"00"
  val lw_bits = lw_off ## rs1p ## B"010" ## rs2p ## B"0000011"

  // C.LD (opIdx=3)
  val ld_off = B"0000" ## x(6 downto 5) ## x(12 downto 10) ## B"000"
  val ld_bits = ld_off ## rs1p ## B"011" ## rs2p ## B"0000011"

  // C.SW (opIdx=6)
  val sw_imm = B"00000" ## x(5) ## x(12 downto 10) ## x(6) ## B"00"
  val sw_bits = sw_imm(11 downto 5) ## rs2p ## rs1p ## B"010" ## sw_imm(4 downto 0) ## B"0100011"

  // C.SD (opIdx=7)
  val sd_imm = B"0000" ## x(6 downto 5) ## x(12 downto 10) ## B"000"
  val sd_bits = sd_imm(11 downto 5) ## rs2p ## rs1p ## B"011" ## sd_imm(4 downto 0) ## B"0100011"

  // C.ADDI/NOP (opIdx=8)
  val addi_imm = B(x(12), 6 bits) ## x(12) ## x(6 downto 2)
  val addi_bits = addi_imm ## rd.asBits ## B"000" ## rd.asBits ## B"0010011"
  val addi_valid = rd =/= 0 || addi_imm.asUInt =/= 0

  // C.ADDIW (opIdx=9)
  val addiw_bits = addi_imm ## rd.asBits ## B"000" ## rd.asBits ## B"0011011"

  // C.LI (opIdx=10)
  val li_bits = addi_imm ## x0.asBits ## B"000" ## rd.asBits ## B"0010011"

  // C.LUI/ADDI16SP (opIdx=11)
  val lui_imm = B(x(12), 14 bits) ## x(12) ## x(6 downto 2) ## B"000000000000"
  val lui_bits = lui_imm(31 downto 12) ## rd.asBits ## B"0110111"
  val addi16sp_imm = B(x(12), 3 bits) ## x(4 downto 3) ## x(5) ## x(2) ## x(6) ## B"0000"
  val addi16sp_bits = addi16sp_imm ## sp.asBits ## B"000" ## sp.asBits ## B"0010011"

  // C.SRLI/SRAI/ANDI (opIdx=12, funct2=0,1,2)
  val shamtV = B"000000" ## x(12) ## x(6 downto 2)
  val srli_bits = shamtV ## rs1p ## B"101" ## rs1p ## B"0010011"
  val srai_bits = (srli_bits.asUInt | U(BigInt(1) << 30)).asBits
  val andi_imm = B(x(12), 6 bits) ## x(12) ## x(6 downto 2)
  val andi_bits = andi_imm ## rs1p ## B"111" ## rs1p ## B"0010011"

  // C.SUB/XOR/OR/AND/SUBW/ADDW (opIdx=12, funct2=3)
  val fIdx = (x(12) ## x(6 downto 5)).asUInt
  val fTbl = Vec(U(0, 3 bits), U(4), U(6), U(7), U(0), U(0), U(2), U(3))
  val rgroup_f3 = fTbl(fIdx)
  val rgroup_opc = x(12) ? B"0111011" | B"0110011"
  val rgroup_base = B"0000000" ## rs2p ## rs1p ## rgroup_f3.resize(3 bits) ## rs1p ## rgroup_opc
  val rgroup_bits = Mux(rgroup_f3 === U(0) && (x(12) || !x(6 downto 5).asUInt.andR),
    (rgroup_base.asUInt | U(BigInt(1) << 30)).asBits, rgroup_base)

  // C.J (opIdx=13)
  val j_off = B(x(12), 10 bits) ## x(12) ## x(8) ## x(10 downto 9) ## x(6) ## x(7) ## x(2) ## x(11) ## x(5 downto 3) ## B"0"
  val j_bits = j_off(20) ## j_off(10 downto 1) ## j_off(11) ## j_off(19 downto 12) ## x0.asBits ## B"1101111"

  // C.BEQZ (opIdx=14)
  val b_off14 = B(x(12), 4 bits) ## x(12) ## x(6 downto 5) ## x(2) ## x(11 downto 10) ## x(4 downto 3) ## B"0"
  val beqz_bits = b_off14(12) ## b_off14(10 downto 5) ## x0.asBits ## rs1p ## B"000" ## b_off14(4 downto 1) ## b_off14(11) ## B"1100011"

  // C.BNEZ (opIdx=15)
  val bnez_bits = b_off14(12) ## b_off14(10 downto 5) ## x0.asBits ## rs1p ## B"001" ## b_off14(4 downto 1) ## b_off14(11) ## B"1100011"

  // C.SLLI (opIdx=16)
  val slli_bits = shamtV ## rd.asBits ## B"001" ## rd.asBits ## B"0010011"

  // C.LWSP (opIdx=18)
  val lwsp_off = B"0000" ## x(3 downto 2) ## x(12) ## x(6 downto 4) ## B"00"
  val lwsp_bits = lwsp_off ## sp.asBits ## B"010" ## rd.asBits ## B"0000011"

  // C.LDSP (opIdx=19)
  val ldsp_off = B"000000" ## x(4 downto 2) ## x(12) ## x(6 downto 5)
  val ldsp_bits = ldsp_off ## sp.asBits ## B"011" ## rd.asBits ## B"0000011"

  // C.JR/MV/JALR/ADD (opIdx=20)
  val cr_add_bits  = B"0000000" ## rs2u.asBits.resize(5 bits) ## rd.asBits ## B"000" ## rd.asBits ## B"0110011"
  val cr_jalr_bits = B"000000000000" ## rd.asBits ## B"000" ## ra.asBits ## B"1100111"
  val cr_mv_bits   = B"0000000" ## rs2u.asBits.resize(5 bits) ## x0.asBits ## B"000" ## rd.asBits ## B"0110011"
  val cr_jr_bits   = B"000000000000" ## rd.asBits ## B"000" ## x0.asBits ## B"1100111"

  // C.SWSP (opIdx=22)
  val swsp_imm = B"0000" ## x(8 downto 7) ## x(12 downto 9) ## B"00"
  val swsp_bits = swsp_imm(11 downto 5) ## rs2u.asBits.resize(5 bits) ## sp.asBits ## B"010" ## swsp_imm(4 downto 0) ## B"0100011"

  // C.SDSP (opIdx=23)
  val sdsp_imm = B"000" ## x(9 downto 7) ## x(12 downto 10) ## B"000"
  val sdsp_bits = sdsp_imm(11 downto 5) ## rs2u.asBits.resize(5 bits) ## sp.asBits ## B"011" ## sdsp_imm(4 downto 0) ## B"0100011"

  // ---- Select outputs based on opIdx ----
  val result = ExpandedInstruction()

  // Default: all zeros + ill for unused opcodes
  val isQuad0_1 = opIdx <= 23 && opIdx =/= 1 && opIdx =/= 4 && opIdx =/= 5 && opIdx =/= 17 && opIdx =/= 21

  // Build select signals for each supported opcode
  def whenOp(n: Int): Bool = opIdx === U(n)

  result.bits := 0
  result.rd   := 0
  result.rs1  := 0
  result.rs2  := 0
  result.rs3  := 0
  io.ill := False

  // opIdx 0: C.ADDI4SPN
  when(whenOp(0)) {
    result.bits := adi4spn_bits
    result.rd   := rs2p.asUInt
    result.rs1  := sp
    io.ill      := !adi4spn_valid
  }
  // opIdx 1,4,5: illegal
  when(whenOp(1) || whenOp(4) || whenOp(5)) { io.ill := True }
  // opIdx 2: C.LW
  when(whenOp(2)) {
    result.bits := lw_bits; result.rd := rs2p.asUInt; result.rs1 := rs1p.asUInt
  }
  // opIdx 3: C.LD
  when(whenOp(3)) {
    result.bits := ld_bits; result.rd := rs2p.asUInt; result.rs1 := rs1p.asUInt
  }
  // opIdx 6: C.SW
  when(whenOp(6)) { result.bits := sw_bits }
  // opIdx 7: C.SD
  when(whenOp(7)) { result.bits := sd_bits }
  // opIdx 8: C.ADDI/NOP
  when(whenOp(8)) {
    result.bits := addi_bits; result.rd := rd; result.rs1 := rd
  }
  // opIdx 9: C.ADDIW
  when(whenOp(9)) {
    when(rd =/= 0) {
      result.bits := addiw_bits; result.rd := rd; result.rs1 := rd
    } otherwise { io.ill := True }
  }
  // opIdx 10: C.LI
  when(whenOp(10)) {
    result.bits := li_bits; result.rd := rd; result.rs1 := x0
  }
  // opIdx 11: C.LUI/ADDI16SP
  when(whenOp(11)) {
    when(rd === sp) {
      result.bits := addi16sp_bits; result.rd := sp; result.rs1 := sp
    } elsewhen(rd =/= 0) {
      result.bits := lui_bits; result.rd := rd
    } otherwise { io.ill := True }
  }
  // opIdx 12: C.SRLI/SRAI/ANDI/R-type
  when(whenOp(12)) {
    switch(x(11 downto 10).asUInt) {
      is(U(0)) { result.bits := srli_bits; result.rd := rs1p.asUInt; result.rs1 := rs1p.asUInt }
      is(U(1)) { result.bits := srai_bits; result.rd := rs1p.asUInt; result.rs1 := rs1p.asUInt }
      is(U(2)) { result.bits := andi_bits; result.rd := rs1p.asUInt; result.rs1 := rs1p.asUInt }
      default  { result.bits := rgroup_bits; result.rd := rs1p.asUInt; result.rs1 := rs1p.asUInt; result.rs2 := rs2p.asUInt }
    }
  }
  // opIdx 13: C.J
  when(whenOp(13)) { result.bits := j_bits }
  // opIdx 14: C.BEQZ
  when(whenOp(14)) { result.bits := beqz_bits; result.rs1 := rs1p.asUInt }
  // opIdx 15: C.BNEZ
  when(whenOp(15)) { result.bits := bnez_bits; result.rs1 := rs1p.asUInt }
  // opIdx 16: C.SLLI
  when(whenOp(16)) {
    when(rd =/= 0) {
      result.bits := slli_bits; result.rd := rd; result.rs1 := rd
    } otherwise { io.ill := True }
  }
  // opIdx 17: illegal (C.FLDSP)
  when(whenOp(17)) { io.ill := True }
  // opIdx 18: C.LWSP
  when(whenOp(18)) {
    when(rd =/= 0) {
      result.bits := lwsp_bits; result.rd := rd; result.rs1 := sp
    } otherwise { io.ill := True }
  }
  // opIdx 19: C.LDSP
  when(whenOp(19)) {
    when(rd =/= 0) {
      result.bits := ldsp_bits; result.rd := rd; result.rs1 := sp
    } otherwise { io.ill := True }
  }
  // opIdx 20: C.JR/MV/JALR/ADD
  when(whenOp(20)) {
    when(x(12)) {
      when(rs2u.orR) {
        result.bits := cr_add_bits; result.rd := rd; result.rs1 := rd; result.rs2 := rs2u
      } otherwise {
        result.bits := cr_jalr_bits; result.rd := ra; result.rs1 := rd
      }
    } otherwise {
      when(rs2u.orR) {
        result.bits := cr_mv_bits; result.rd := rd; result.rs1 := x0; result.rs2 := rs2u
      } otherwise {
        result.bits := cr_jr_bits; result.rs1 := rd
      }
    }
  }
  // opIdx 21: illegal (C.JAL)
  when(whenOp(21)) { io.ill := True }
  // opIdx 22: C.SWSP
  when(whenOp(22)) { result.bits := swsp_bits }
  // opIdx 23: C.SDSP
  when(whenOp(23)) { result.bits := sdsp_bits }

  io.instOut := result
}

// =============================================================================
// Generator
// =============================================================================

object GenRVCExpander {
  def main(args: Array[String]): Unit = {
    SpinalConfig(
      mode = SystemVerilog,
      targetDirectory = "rtl",
      genLineComments = true,
      oneFilePerComponent = true,
      withTimescale = false,
      printFilelist = false
    ).generate {
      new RVCExpander
    }
    println("Generated rtl/RVCExpander.sv")
  }
}
