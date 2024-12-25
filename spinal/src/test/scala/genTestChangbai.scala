package changbaiV1.v1.test

import spinal.core.{SpinalConfig, SystemVerilog}

object Play {
  def main(args: Array[String]) {
    println("test Gen Misc......")
    SpinalConfig(mode = SystemVerilog, targetDirectory = "changbaiTest", genLineComments = true, oneFilePerComponent = true)
      .generate {
        val topLevel = new MyPlay
        topLevel
      }

  }
}

object Print {
  def main(args: Array[String]) {
    println("test Gen Changbai......")
  }
}