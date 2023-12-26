package sandbox
import chisel3._

class Hello extends Module {
  val io = IO(new Bundle {
    val out = Output(UInt(10.W))
  })
  println(io.out.getWidth)
  io.out := 42.U
}
