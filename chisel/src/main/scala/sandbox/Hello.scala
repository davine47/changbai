package sandbox

import chisel3._
import chisel3.util.{Fill, Pipe}
import freechips.rocketchip.amba.axi4.AXI4Bundle
import org.chipsalliance.cde.config.Parameters

class Hello()(implicit p:Parameters) extends Module {

  val io = IO(new Bundle {
    val in = Input(UInt(8.W))
    val out = Output(UInt(8.W))
  })
  val pipe = Module(new Pipe(UInt(8.W),40))
  AXI4Bundle
  println(io.out.getWidth)
  pipe.io.enq.bits := io.in
  pipe.io.enq.valid := true.B
  io.out := pipe.io.deq.bits
}

