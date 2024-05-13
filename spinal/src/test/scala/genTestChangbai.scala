package changbai.v1.test

import spinal.core.{SpinalConfig, SystemVerilog}
import v1.IDecode

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