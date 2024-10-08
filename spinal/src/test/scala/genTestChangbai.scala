package changbaiV1.v1.test

import spinal.core.{SpinalConfig, SystemVerilog}
import v1.IDecode
import v1.fn.{ALUFull, ALULite, ALUSum}
import v1.pipe.SimpleAluRs

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

object SimpleRs {
  def main(args: Array[String]) {
    println("test Gen SimpleRs......")
    SpinalConfig(mode = SystemVerilog, targetDirectory = "changbaiTest", genLineComments = false, oneFilePerComponent = true)
      .generate {
        val topLevel = new SimpleAluRs()
        topLevel
      }
  }
}

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