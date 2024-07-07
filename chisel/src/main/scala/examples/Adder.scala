package examples

import chisel3._

class Adder() extends RawModule {
  val io = IO(new Bundle {
    val a = Input(UInt(8.W))
    val b = Input(UInt(8.W))
    val c = Output(UInt(8.W))
  })

  io.c := io.a + io.b
}
