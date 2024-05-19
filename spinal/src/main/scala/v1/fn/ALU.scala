package v1.fn

import spinal.core.{Area, B, Bits, Bundle, Cat, Component, IntToBuilder, Mux, S, U, in, out}
import spinal.lib.Reverse
import v1.{FnService, RiscvUnPrivSpec, SrcLessUnsignedService, SrcUseSubLess}

trait FnMatrix {
  def position: (Int, Int)
}

/**
  * ALUBaseBundle:
  *   sum_res and cmp_res will benefit to critical path
  */
abstract class ALUBaseBundle extends Bundle {
  val a = Bits(RiscvUnPrivSpec.XLEN bits)
  val b = Bits(RiscvUnPrivSpec.XLEN bits)
  val res = Bits(RiscvUnPrivSpec.XLEN bits)
  val uAcmd = Bits(SrcUseSubLess.range.size + SrcLessUnsignedService.range.size +
    FnService.range.size bits) //TODO: need framework factory
}

class ALUBundle extends ALUBaseBundle {
  a.asInput()
  b.asInput()
  uAcmd.asInput()
  res.asOutput()
}

class ALUBitwiseArea extends Area {
  object signals extends ALUBaseBundle
  // OR XOR AND
  val or = signals.a | signals.b
  val xor = signals.a ^ signals.b
  val and = signals.a & signals.b
  val bitwise = signals.uAcmd(FnService.ALU_FUNC_RANGE2).asUInt.mux(
    0 -> or,
    1 -> xor,
    2 -> and,
    3 -> signals.a
  )
}

class ALUShiftArea extends Area {
  object signals extends ALUBaseBundle
  // SLL SRL SRA
  val amplitude32  = signals.b(4 downto 0).asUInt
  val amplitude = if(RiscvUnPrivSpec.XLEN == 32) amplitude32 else signals.b(5 downto 0).asUInt
  val reversed   = Mux(~(signals.uAcmd(FnService.ALU_FUNC_RANGE2).asBits.orR), Reverse(signals.a), signals.a)
  val sr = (Cat(signals.uAcmd(FnService.ALU_FUNC_RANGE2) === B("10") & reversed.msb, reversed).asSInt >> amplitude)(RiscvUnPrivSpec.XLEN-1 downto 0).asBits
  val sl = Reverse(sr)
  val shift = Mux(~signals.uAcmd(FnService.ALU_FUNC_RANGE2).asBits.orR, sl, sr)
}
/**
  * ALU:
  *   Implementation with Matrix micro-arch
  * @param position
  */
class ALU extends Component {

  val io = new ALUBundle
  // ADD SUB
  val addSub = (io.a.asSInt + Mux(io.uAcmd(SrcUseSubLess.range).asBool, ~io.b, io.b).asSInt +
    Mux(io.uAcmd(SrcUseSubLess.range).asBool, S(1, 32 bits), S(0, 32 bits))).asBits

  // SLT SLTU
  val less = Mux(io.a.msb === io.b.msb, addSub.msb,
    Mux(io.uAcmd(SrcLessUnsignedService.range).asBool, io.b.msb, io.a.msb))

  // bitwise area
  val bitwiseArea = new ALUBitwiseArea
  bitwiseArea.signals.a := io.a
  bitwiseArea.signals.b := io.b
  bitwiseArea.signals.uAcmd := io.uAcmd
  val bitwise = bitwiseArea.bitwise

  // shift area
  val shiftArea = new ALUShiftArea
  shiftArea.signals.a := io.a
  shiftArea.signals.b := io.b
  shiftArea.signals.uAcmd := io.uAcmd
  val shift = shiftArea.shift

  io.res := io.uAcmd(FnService.ALU_FUNC_RANGE1).asBits.mux(
    B("00") -> addSub,
    B("01") -> less.asBits(RiscvUnPrivSpec.XLEN bit),
    B("10") -> bitwise,
    B("11") -> shift
  )
}
