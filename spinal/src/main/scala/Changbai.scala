package changbaiV1
import spinal.core._
import spinal.lib._
import lib._

class ChangbaiV1 extends Component with Pipeline {

  type T = ChangbaiV1
  def newStage(): Stage = { val s = new Stage; stages += s; s }
  val stage1 = newStage()
  val stage2 = newStage()
  val stage3 = newStage()
  val stage4 = newStage()

  plugins += new SingPlugin
}

class SingPlugin extends Plugin[ChangbaiV1] {

  object RES extends Stageable(UInt(32 bits))
  object FORWARD_A extends Stageable(UInt(32 bits))
  object RES2 extends Stageable(UInt(32 bits))
  object RES3 extends Stageable(UInt(32 bits))

  override def setup(pipeline: ChangbaiV1): Unit = {
    println("software setup")
  }

  override def build(pipeline: ChangbaiV1): Unit = {
    import pipeline._
    stage1 plug new Area {
      import stage1._
      val a, b = UInt(32 bits)
      a := 3
      b := 6
      stage3.insert(FORWARD_A) := a + b
      insert(RES) := a - b
    }

    stage3 plug new Area {
      import stage3._
      //val c = input(RES)
      val c = U(9)
      val res = c + input(FORWARD_A)
    }

    stage4 plug new Area {
      import stage4._
      //val d = input(RES2)
      //insert(RES3) := d + U(5)
    }
  }
}

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
        val topLevel = new ChangbaiV1
        topLevel
      }
  }
}

