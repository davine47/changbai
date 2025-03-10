package changbaiV1
import lib.{Pipeline, Stage}
import spinal.core._
import spinal.core.in.Bool
import spinal.lib._

class CoreTop extends Component with Pipeline {
  type T = CoreTop

  def newStage(): Stage = { val s = new Stage; stages += s; s }

  val s0 = newStage()
  val s1 = newStage()
  val s2 = newStage()
  val s3 = newStage()


}

class Test extends Component {
  val io = new Bundle {
    val in = spinal.core.in Bool()
    val out = spinal.core.out Bool()
  }
  noIoPrefix()
  withoutReservedKeywords = true
  io.out := False | io.in
}

object sayHello {
  def main(args: Array[String]): Unit = {
    println("Hello this is changbai env!")
    SpinalConfig(mode = SystemVerilog, targetDirectory = "changbaiTest", genLineComments = false, oneFilePerComponent = true)
      .generate {
        val topLevel = new Test()
        topLevel
      }
  }
}

object genChangbai {
  def main(args: Array[String]) {
    println("Gen Changbai......")
  }
}

