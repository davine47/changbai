package v1.fn

import spinal.core.{Area, B, Bits, Bundle, Cat, Component, IntToBuilder, Mux, S, U, in, out}
import spinal.lib.Reverse
import v1.{FnService, RiscvUnPrivSpec, SrcLessUnsignedService, SrcUseSubLess}

trait FnMatrix {
  def position: (Int, Int)
}

/**
  * ALUBaseBundle:
  * Use to formalize the shapes of ALU areas' signals
  */
abstract class ALUBaseBundle extends Bundle {
  val a = Bits(RiscvUnPrivSpec.XLEN bits)
  val b = Bits(RiscvUnPrivSpec.XLEN bits)
  val res = Bits(RiscvUnPrivSpec.XLEN bits)
  val uAcmd = Bits(SrcUseSubLess.range.size + SrcLessUnsignedService.range.size +
    FnService.range.size bits) //TODO: [optimize] need framework factory

  def connectIn(that: ALUBaseBundle) = new Area {
    a := that.a
    b := that.b
    uAcmd := that.uAcmd
  }
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

class ALUSumArea extends Area {
  object signals extends ALUBaseBundle

  val addSub = (signals.a.asSInt + Mux(signals.uAcmd(SrcUseSubLess.range).asBool, ~signals.b, signals.b).asSInt +
    Mux(signals.uAcmd(SrcUseSubLess.range).asBool, S(1, RiscvUnPrivSpec.XLEN bits), S(0, RiscvUnPrivSpec.XLEN bits))).asBits
}

class ALUSumAndCmpArea extends Area {
  object signals extends ALUBaseBundle
  // ADD SUB
  val addSubArea = new ALUSumArea
  addSubArea.signals.connectIn(signals)
  val addSub = addSubArea.addSub
  // SLT SLTU
  val less = Mux(signals.a.msb === signals.b.msb, addSub.msb,
    Mux(signals.uAcmd(SrcLessUnsignedService.range).asBool, signals.b.msb, signals.a.msb))
}

class ALUSum extends Component {
  val io = new ALUBundle

  val sumArea = new ALUSumArea
  sumArea.signals.connectIn(io)
  io.res := sumArea.addSub
}

class ALULite extends Component {
  val io = new ALUBundle

  // sum and cmp area
  val sumAndCmpArea = new ALUSumAndCmpArea
  sumAndCmpArea.signals.connectIn(io)
  val addSub = sumAndCmpArea.addSub
  val less = sumAndCmpArea.less

  io.res := io.uAcmd(FnService.ALU_FUNC_RANGE1).lsb.asBits.mux(
    B("0") -> addSub,
    B("1") -> less.asBits(RiscvUnPrivSpec.XLEN bit)
  )
}

/**
  * ALU:
  *   Implementation with Matrix micro-arch
  */
class ALUFull extends Component {

  val io = new ALUBundle

  // sum and cmp area
  val sumAndCmpArea = new ALUSumAndCmpArea
  sumAndCmpArea.signals.connectIn(io)
  val addSub = sumAndCmpArea.addSub
  val less = sumAndCmpArea.less

  // bitwise area
  val bitwiseArea = new ALUBitwiseArea
  bitwiseArea.signals.connectIn(io)
  val bitwise = bitwiseArea.bitwise

  // shift area
  val shiftArea = new ALUShiftArea
  shiftArea.signals.connectIn(io)
  val shift = shiftArea.shift

  io.res := io.uAcmd(FnService.ALU_FUNC_RANGE1).asBits.mux(
    B("00") -> addSub,
    B("01") -> less.asBits(RiscvUnPrivSpec.XLEN bit),
    B("10") -> bitwise,
    B("11") -> shift
  )
}
