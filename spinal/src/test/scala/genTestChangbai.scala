package changbai.v1.test

import spinal.core.{SpinalConfig, SystemVerilog}
import v1.IDecode
import v1.fn.ALU

object ALU extends App{
  println("Gen ChangBai ALU......")
  SpinalConfig(mode = SystemVerilog, targetDirectory = "changbaiTest", genLineComments = true, oneFilePerComponent = true)
    .generate {
      val topLevel = new ALU
      topLevel
    }
}
object IDecoder extends App{
  println("Gen ChangBai IDecoder......")
  SpinalConfig(mode = SystemVerilog, targetDirectory = "changbaiTest", genLineComments = true, oneFilePerComponent = true)
    .generate {
      val topLevel = new IDecode
      topLevel
    }
}

object genTestChangbai {
  def main(args: Array[String]) {
    println("test Gen Changbai......")
  }
}