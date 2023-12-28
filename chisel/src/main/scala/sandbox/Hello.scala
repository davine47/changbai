package sandbox

import chisel3._
import org.chipsalliance.cde.config.Parameters

class Hello()(implicit p:Parameters) extends Module {

  val io = IO(new Bundle {
    val out = Output(UInt(10.W))
  })
  println(io.out.getWidth)
  io.out := 42.U
}
