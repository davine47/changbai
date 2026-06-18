// See README.md for license details.

package examples

import chisel3._
import _root_.circt.stage.ChiselStage

class MyBundle() extends Bundle {
  val isRight = Bool()
  val isLeft = Bool()
  val sleep = Bool()
}

object params {
  val a = false
  val b = false
}

class MyModule() extends Module {
  val io = IO(new Bundle {
    val my = Input(new MyBundle)
    val value = Output(Bool())
  })

  val x  = RegInit(false.B)
  val y  = Reg(new MyBundle)

  y.isLeft := io.my.isLeft
  y.isRight := io.my.isRight

  // y.sleep := DontCare
  dontTouch(y.sleep)
  if (params.a) {
    y.sleep := io.my.sleep
  }

  if (params.b) {
    y.sleep := io.my.isLeft
  }
  
  io.value := !y.sleep && y.isRight && !y.isLeft

  // val yNext = WireInit(y)

  // yNext.isLeft := io.my.isLeft
  // yNext.isRight := io.my.isRight
  // yNext.sleep := DontCare

  // io.value := !yNext.sleep && yNext.isRight && !yNext.isLeft
}

/**
 * Generate Verilog sources and save it in file GCD.v
 */
object MyModule extends App {
  ChiselStage.emitSystemVerilogFile(
    new MyModule,
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info", "-default-layer-specialization=enable")
  )
}
