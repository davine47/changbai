package v1.fn

import spinal.core.{Bundle, Component, IntToBuilder, in, out}
import v1.{FnService, RiscvUnPrivSpec}

trait FnMatrix {
  def position: (Int, Int)
}

/**
  * ALUBundle:
  *   sum_res and cmp_res will benefit to critical path
  */
class ALUBundle extends Bundle {
  val a       = in  Bits(RiscvUnPrivSpec.XLEN bits)
  val b       = in  Bits(RiscvUnPrivSpec.XLEN bits)
  val res     = out Bits(RiscvUnPrivSpec.XLEN bits)
  val sum_res = out Bits(RiscvUnPrivSpec.XLEN bits)
  val cmp_res = out Bits(RiscvUnPrivSpec.XLEN bits)
  val sel     = in  Bits(FnService.ALU_SEL_WIDTH bits)
}

/**
  * ALU:
  *   Implementation with Matrix micro-arch
  * @param position
  */
class ALU(position: (Int, Int)) extends Component with FnMatrix {

  override def position: (Int, Int) = position
  // ADD SUB


}
