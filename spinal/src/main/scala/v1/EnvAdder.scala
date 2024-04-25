package v1

import changbai.Changbai
import spinal.core.internals.Operator
import spinal.core.{Component, IntToBuilder, SpinalConfig, SystemVerilog, UInt, in, out}
import spinal.lib.experimental.chisel.Bundle

class EnvAdder() extends Component{
  val io = new Bundle{
    val a, b = in UInt(8 bits)
    val sum = out UInt(8 bits)
  }
  io.sum := io.a + io.b
}

object genAdder {
  def main(args: Array[String]) {
    println("Gen Adder......")
    SpinalConfig(mode = SystemVerilog, targetDirectory = "adder", oneFilePerComponent = true)
      .generate {
        val topLevel = new EnvAdder
        topLevel
      }
  }
}

