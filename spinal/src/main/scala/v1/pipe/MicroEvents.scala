package v1.pipe

import spinal.core._
import spinal.lib._
import v1.{FnService, RiscvUnPrivSpec, SimpleRobService, SimpleRsService, SrcLessUnsignedService, SrcUseSubLess}

abstract class MicroEvent extends Bundle

trait NoEventConverter

class RsUpdateMicroEvent extends MicroEvent {
  val tag = UInt(SimpleRsService.TAG_WIDTH bits)
  val data = UInt(RiscvUnPrivSpec.XLEN bits)
}

class RsAllocateReqMicroEvent extends MicroEvent {
  val robIdx = UInt(SimpleRobService.ROB_IDX_WIDTH bits)
  val uAcmd = Bits(SrcUseSubLess.range.size + SrcLessUnsignedService.range.size +
    FnService.range.size bits) //TODO: [optimize] need framework factory
}

class RsIssueMicroEvent extends MicroEvent {
  val src0 = UInt(RiscvUnPrivSpec.XLEN bits)
  val src1 = UInt(RiscvUnPrivSpec.XLEN bits)
}