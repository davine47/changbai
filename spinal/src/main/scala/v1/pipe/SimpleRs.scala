package v1.pipe

import spinal.core._
import spinal.lib._
import v1.{RiscvUnPrivSpec, SimpleRobService, SimpleRsService}

class SimpleAluRs(entryCounts : Int = 4) extends Component {
  val io = new Bundle {
    // bus channels
    val updateE0 = slave Flow(new RsUpdateMicroEvent)
    val updateE1 = slave Flow(new RsUpdateMicroEvent)
    val updateE2 = slave Flow(new RsUpdateMicroEvent)
    // allocate req/resp
    val allocateReqE0 = slave Stream(new RsAllocateReqMicroEvent)
  }

  case class RsEntry() extends Bundle {
    val idx = UInt(log2Up(entryCounts) bits)
    val tag0 = UInt(SimpleRsService.TAG_WIDTH bits)
    val data0 = UInt(RiscvUnPrivSpec.XLEN bits)
    val valid0 = Bool()
    val tag1 = UInt(SimpleRsService.TAG_WIDTH bits)
    val data1 = UInt(RiscvUnPrivSpec.XLEN bits)
    val valid1 = Bool()
    val robIdx = UInt(SimpleRobService.ROB_IDX_WIDTH bits)
  }

  // base ctrl regs
  val entries_r = Vec.fill(entryCounts)(Reg(RsEntry()))
  val entriesValids_r = Vec.fill(entryCounts)(Reg(Bool()))
  entries_r.zipWithIndex.foreach(x => {
    x._1.idx := x._2
  })

  // issue
  val canIssueBitsVec = entries_r.map(x => x.valid0 & x.valid1)
  val canIssue = canIssueBitsVec.reduce(_ | _)
  val canIssueEntry = PriorityMux(canIssueBitsVec, entries_r)
  val canIssueIdx = canIssueEntry.idx

  // allocate
  val full = entriesValids_r.reduce(_ & _)
  val canAccept = ~full | full & canIssue
  val freeBitsVec = entriesValids_r.map(x => ~x)
  val freeEntry = PriorityMux(freeBitsVec, entries_r)
  val freeEntryValid = PriorityMux(freeBitsVec, entriesValids_r)
  when(io.allocateReqE0.fire) {
    when(full & canIssue) {
      entries_r(canIssueIdx).robIdx := io.allocateReqE0.robIdx
      entriesValids_r(canIssueIdx) := True // keep valid
    }.otherwise {
      freeEntry.robIdx := io.allocateReqE0.robIdx
      freeEntryValid := True
    }
  }

  // hit
  //entries_r.foreach(x => {
  //  x.valid0 := True
  //  x.valid1 := True
  //})

  // update


  io.allocateReqE0.ready := canAccept




}
