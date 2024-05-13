package v1

import changbai.Changbai
import spinal.core.internals.Operator
import spinal.core.{B, Bits, Component, HardType, IntToBuilder, LiteralBuilder, Mem, SpinalConfig, SpinalVerilog, SystemVerilog, U, UInt, Verilator, in, out}
import spinal.lib.experimental.chisel.Bundle
import spinal.lib.logic.{DecodingSpec, Masked}

class RegFileExample extends Component{

  val io = new Bundle {
    val raddr0 = in UInt(5 bits)
    val rdata0 = out UInt(64 bits)
    val raddr1 = in UInt(5 bits)
    val rdata1 = out UInt(64 bits)
    val wen = in Bool()
    val waddr = in UInt(5 bits)
    val wdata = in UInt(64 bits)
  }

  val regFile = Mem(Bits(64 bits), 32) addAttribute(Verilator.public)

  io.rdata0 := regFile.readSync(io.raddr0).asUInt
  io.rdata1 := regFile.readSync(io.raddr1).asUInt
  regFile.write(io.waddr, io.wdata.asBits, io.wen)
}

object RegFileExample extends App{
  println("Gen regfile example......")
  SpinalConfig(mode = SystemVerilog, targetDirectory = "examples", genLineComments = true, oneFilePerComponent = true)
    .generate {
      val topLevel = new RegFileExample
      topLevel
    }
}

class AdderExample() extends Component{
  val io = new Bundle{
    val a, b = in UInt(8 bits)
    val sum = out UInt(8 bits)
  }
  io.sum := io.a + io.b
}

class DecodingSpecExample extends Component {
  val spec = new DecodingSpec(HardType(UInt(5 bits)))
  val m000 = M"000"
  val m100 = M"1-0"
  val m101 = M"101"
  val m111 = M"111"
  spec.setDefault(Masked(U"00011"))
  spec.addNeeds(Masked(m000), Masked(U"11000"))
  spec.addNeeds(Masked(m100), Masked(U"11100"))
  spec.addNeeds(Masked(m101), Masked(U"10101"))
  spec.addNeeds(Masked(m111), Masked(U"11101"))

  val sel = in Bits (3 bits)
  val result = out UInt (5 bits)
  result := spec.build(sel, Seq(Masked(B"011"), Masked(B"100")))
}

object DecodingSpecExample extends App{
  println("Gen Decode example......")
  SpinalConfig(mode = SystemVerilog, targetDirectory = "examples", genLineComments = true, oneFilePerComponent = true)
    .generate {
      val topLevel = new DecodingSpecExample
      topLevel
    }
}

object AdderExample extends App{
  println("Gen Adder......")
  SpinalConfig(mode = SystemVerilog, targetDirectory = "examples", oneFilePerComponent = true)
    .generate {
      val topLevel = new AdderExample
      topLevel
    }
}

