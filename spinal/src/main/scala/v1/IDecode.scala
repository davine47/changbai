package v1

import spinal.core.{B, Bits, Bundle, Component, False, HardType, IntToBuilder, LiteralBuilder, MaskedLiteral, U, UInt, default, in, out}
import Instructions._
import spinal.lib.logic.{DecodingSpec, Masked}

import scala.language.postfixOps

class IDecodeBundle extends Bundle {
  val rawInst = in Bits(32 bits)
  val result = out Bits(DecodeService.targetWidth bits)
}

class IDecode extends Component {
  val io = new IDecodeBundle

  val spec = new DecodingSpec(HardType(Bits(DecodeService.targetWidth bits)))

  // build DecodeMap list
  spec.setDefault(Masked(DecodeService.mergedDefaultML))

  val serviceList = List(FnService)
  RV64I.foreach(inst => {
    spec.addNeeds(Masked(inst), Masked(DecodeService.instMapOpcode.get(inst).get))
  })
  io.result := spec.build(io.rawInst, RV64I.flatMap(x => List(Masked(x.asBits()))))

}
