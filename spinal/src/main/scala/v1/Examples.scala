package v1

import changbai.Changbai
import spinal.core.internals.Operator
import spinal.core.{B, Component, HardType, IntToBuilder, LiteralBuilder, SpinalConfig, SpinalVerilog, SystemVerilog, U, UInt, in, out}
import spinal.lib.experimental.chisel.Bundle
import spinal.lib.logic.{DecodingSpec, Masked}

class AdderExample() extends Component{
  val io = new Bundle{
    val a, b = in UInt(8 bits)
    val sum = out UInt(8 bits)
  }
  io.sum := io.a + io.b
}

class DecodingSpecExample extends Component {
  val spec = new DecodingSpec(HardType(UInt(5 bits)))
  spec.setDefault(Masked(U"00011"))
  spec.addNeeds(Masked(B"000"), Masked(U"11000"))
  spec.addNeeds(Masked(B"100"), Masked(U"11100"))
  spec.addNeeds(Masked(B"101"), Masked(U"10101"))
  spec.addNeeds(Masked(B"111"), Masked(U"11101"))

  val sel = in Bits (3 bits)
  val result = out UInt (5 bits)
  result := spec.build(sel, Seq(Masked(B"011"), Masked(B"100")))
}

object DecodingSpecExample extends App{
  println("Gen Decode example......")
  SpinalConfig(mode = SystemVerilog, targetDirectory = "decode", genLineComments = true, oneFilePerComponent = true)
    .generate {
      val topLevel = new DecodingSpecExample
      topLevel
    }
}

object AdderExample extends App{
  println("Gen Adder......")
  SpinalConfig(mode = SystemVerilog, targetDirectory = "adder", oneFilePerComponent = true)
    .generate {
      val topLevel = new AdderExample
      topLevel
    }
}

