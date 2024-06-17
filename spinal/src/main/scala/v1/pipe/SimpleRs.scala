package v1.pipe

import spinal.core._
import spinal.lib._
import v1.{RiscvUnPrivSpec, SimpleRobService, SimpleRsService}

class SimpleAluRs(entryCounts : Int = 4) extends Component {
  val io = new Bundle {
    // bus channels
    val updateE0 = Flow(new RsUpdateMicroEvent)
    val updateE1 = Flow(new RsUpdateMicroEvent)
    val updateE2 = Flow(new RsUpdateMicroEvent)
    // allocate req/resp
    val allocateReqE0 = Stream(new RsAllocateReqMicroEvent)
  }

  case class RsEntry() extends Bundle {
    val tag0 = UInt(SimpleRsService.TAG_WIDTH bits)
    val tag1 = UInt(SimpleRsService.TAG_WIDTH bits)
    val valid0 = Bool()
    val valid1 = Bool()
    val robIdx = UInt(SimpleRobService.ROB_IDX_WIDTH bits)
    // val associate = UInt(log2Up(8) bits)
  }

  // base ctrl regs
  val entries = Vec.fill(entryCounts)(Reg(RsEntry()))

  // update

  // hit
  val entryPair = entries.sliding(2, 2).toSeq

  // issue




}
