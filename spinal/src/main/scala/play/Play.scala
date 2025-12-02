package play

import play.lib.{Pipeline, Stage}
import spinal.core._
import spinal.lib._

class CoreTop extends Component with Pipeline {
  type T = CoreTop

  def newStage(): Stage = { val s = new Stage; stages += s; s }

  val s0 = newStage()
  val s1 = newStage()
  val s2 = newStage()
  val s3 = newStage()


}

case class TestBundle() extends Bundle {
  val mul = UInt(8 bit)
}

class Test extends Component {
  val io = new Bundle {
    val in = spinal.core.in Bool()
    val out = spinal.core.out Bool()
  }
  val a = Stream(TestBundle())
  noIoPrefix()
  withoutReservedKeywords = true
  io.out := False | io.in
}

object sayHello {
  def main(args: Array[String]): Unit = {
    println("Hello this is changbai env!")
    SpinalConfig(mode = SystemVerilog, targetDirectory = "play", genLineComments = false, oneFilePerComponent = true)
      .generate {
        val topLevel = new Test()
        topLevel
      }
  }
}

