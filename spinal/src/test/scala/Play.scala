package changbaiV1.v1.test
import spinal.core._
import spinal.lib._

class MyPlay extends Component {
  val a = in port Vec.fill(8)(UInt(8 bits))
  val mask = in port UInt(8 bits)
  val b = out port UInt(8 bits)
  b := PriorityMux(mask.asBools, a)
}
