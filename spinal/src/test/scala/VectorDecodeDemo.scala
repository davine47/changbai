package changbaiV1.v1.test

import spinal.core._
import spinal.lib._
import spinal.lib.logic._
import v1.MicroOps._
import v1.Instructions._
import v1.utils.{AbstractDecodeSigs, DecodeConst}

class VectorDecodeBundle extends Bundle {
  val legal = Bool()
  val config = Bool()
  val isMem = Bool()
  val isLogic = Bool()
  val isStride = Bool()
}

class VectorDecodeSigs[T <: BaseType](needs: DecodeConst, coverAll: Seq[Masked], spec: DecodingSpec[T])
  extends AbstractDecodeSigs(needs, coverAll, spec) with Area {

  override val sigs = new VectorDecodeBundle

  override val default: List[MaskedLiteral] = List(
    X, X, X, X, X
  )
}

object VectorDecodeSigs {
  def apply[T <: BaseType](needs: DecodeConst, spec: DecodingSpec[T]): VectorDecodeSigs[T] = {
    val coverAll = needs.table.map(i => Masked(i._1)).toSeq
    new VectorDecodeSigs[T](needs, coverAll, spec)
  }
}

class VectorDecodeMapTableConst extends DecodeConst {
  override val table: Array[(MaskedLiteral, List[MaskedLiteral])] = Array(
    VSETVL -> List(Y, N, N, N),
    VSETIVLI -> List(Y, Y, N, N),
    VSETVLI -> List(Y, Y, Y, N)
  )
}

class VectorDecodeComponent extends Component {
  val i_op = in port Bits(32 bits)
  val o_op = out port Bits(5 bits)

  val spec = new DecodingSpec(HardType(Bits(4 bits))) // (n-1) bits

  val decodeArea = VectorDecodeSigs[Bits](new VectorDecodeMapTableConst, spec)

  val decodeBundle = new VectorDecodeBundle

  decodeBundle.assignFromBits(decodeArea.decode(i_op))
  o_op := decodeBundle.asBits

}