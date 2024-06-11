package v1.pipe

import spinal.core._
import spinal.lib._
import v1.{RiscvUnPrivSpec, SimpleRsService}

class SimpleRs(entryCounts : Int = 16) extends Component {
  val io = new Bundle {
    val updateSrc0 = Flow(new RsUpdateMicroEvent)
    val updateSrc1 = Flow(new RsUpdateMicroEvent)

  }

  case class RsEntry() extends Bundle {
    val tag = UInt(SimpleRsService.TAG_WIDTH bits)
    val valid = Bool()
    // val associate = UInt(log2Up(8) bits)
  }

  val entries = Vec.fill(entryCounts)(Reg(RsEntry()))
  val entryPair = entries.sliding(2, 2).toSeq
  val entryPairValid = entries.sliding(2, 2).collect {
    case Seq(a, b) => a.valid | b.valid
  }.toSeq

  val hitEntryTags = entryPair.map(x => Seq((x(0).tag === io.updateSrc0.tag) & io.updateSrc0.valid,
    (x(1).tag === io.updateSrc1.tag) & io.updateSrc1.valid))
  entryPair.zipWithIndex.foreach(x => {
    x._1(0).valid := hitEntryTags(x._2).head
    x._1(1).valid := hitEntryTags(x._2).tail
  })

  // issue and release
}
