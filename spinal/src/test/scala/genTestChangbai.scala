package changbai.v1.test

import spinal.core.{SpinalConfig, SystemVerilog}
import v1.IDecode
import v1.fn.{ALUFull, ALULite, ALUSum}

object ALU extends App{
  println("Gen ChangBai ALU......")
  val sc = SpinalConfig(mode = SystemVerilog, targetDirectory = "changbaiTest", genLineComments = true, oneFilePerComponent = true)
  sc.generate {
    val topLevel = new ALUFull
    topLevel
  }
  sc.generate{
    val topLevel = new ALUSum
    topLevel
  }
  sc.generate {
    val topLevel = new ALULite
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

object Print {
  def main(args: Array[String]) {
    println("test Gen Changbai......")
  }
}