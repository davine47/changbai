package changbaiV1.v1.test

import spinal.core.{SpinalConfig, SystemVerilog}

object Play {
  def main(args: Array[String]) {
    println("test Gen Misc......")
    SpinalConfig(
      mode = SystemVerilog,
      targetDirectory = "play",
      genLineComments = true,
      oneFilePerComponent = true,
      withTimescale = false,
      printFilelist = false)
      .generate {
        val topLevel = new VectorDecodeComponent
        topLevel
      }

  }
}

object myPrint {
  def main(args: Array[String]) {
    println("test Gen Changbai......")
  }
}