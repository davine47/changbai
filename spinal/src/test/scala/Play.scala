package changbai.v1.test
import spinal.core._
import spinal.lib._

class MyPlay extends Component {
  val a = in UInt(8 bits)
  val b = out UInt(log2Up(8) bits)
  b := OHToUInt(a)
}
