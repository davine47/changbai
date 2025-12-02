package v1.utils

import spinal.core._
import spinal.lib.logic._

abstract trait DecodeConst {
  def unwrap(list: List[MaskedLiteral]): MaskedLiteral = list.reverse.reduce(
    (a, b) => MaskedLiteral(a.getBitsString(a.width, '-') + b.getBitsString(b.width, '-')))

  val table: Array[(MaskedLiteral, List[MaskedLiteral])]
}

abstract class AbstractDecodeSigs[T <: BaseType](needs: DecodeConst, coverAll: Seq[Masked], spec: DecodingSpec[T]) {
  val sigs: Bundle
  val default: List[MaskedLiteral]

  def decode(in: Bits): Bits = {
    needs.table.foreach(i => spec.addNeeds(Masked(i._1), Masked(needs.unwrap(i._2))))
    val legal = Symplify.logicOf(in, SymplifyBit.getPrimeImplicantsByTrueAndDontCare(coverAll, Nil, in.getBitsWidth))
    spec.setDefault(Masked(needs.unwrap(default)))
    val decodeRes = spec.build(in, coverAll).asBits
    val decodeResWithLegal = Cat(decodeRes, legal.asBits)
    sigs.assignFromBits(decodeResWithLegal)
    sigs.asBits
  }
}
