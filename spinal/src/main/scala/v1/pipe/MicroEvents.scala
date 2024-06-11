package v1.pipe

import spinal.core._
import spinal.lib._
import v1.{RiscvUnPrivSpec, SimpleRsService}

abstract class MicroEvent extends Bundle

trait NoEventConverter

class RsUpdateMicroEvent extends MicroEvent {
  val tag = UInt(SimpleRsService.TAG_WIDTH bits)
  val data = UInt(RiscvUnPrivSpec.XLEN bits)
}

class RsIssueMicroEvent extends MicroEvent {
  val src0 = UInt(RiscvUnPrivSpec.XLEN bits)
  val src1 = UInt(RiscvUnPrivSpec.XLEN bits)
}