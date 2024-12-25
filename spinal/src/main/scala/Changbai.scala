package changbaiV1
import spinal.core._
import spinal.lib._

object sayHello {
  def main(args: Array[String]): Unit = {
    println("Hello this is changbai env!")
  }
}

object genChangbai {
  def main(args: Array[String]) {
    println("Gen Changbai......")
    SpinalConfig(mode = SystemVerilog, targetDirectory = "changbai", oneFilePerComponent = true)
      .generate {
      }
  }
}

