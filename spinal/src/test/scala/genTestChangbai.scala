package changbaiV1.v1.test

import spinal.core.{SpinalConfig, SystemVerilog}

object Play {
  def main(args: Array[String]) {
    println("test Gen Misc......")
    SpinalConfig(
      mode = SystemVerilog,
      targetDirectory = "rtl",
      genLineComments = true,
      oneFilePerComponent = true,
      withTimescale = false,
      printFilelist = false)
      .generate {
        val topLevel = new MyPlay
        topLevel
      }

  }
}

object myPrint {
  def main(args: Array[String]) {
    println("test Gen Changbai......")
  }
}