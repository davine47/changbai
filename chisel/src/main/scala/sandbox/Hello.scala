package sandbox

import chisel3.{UInt, _}
import chisel3.util.{Fill, Pipe}
import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp, LazyModuleImpLike}
import org.chipsalliance.cde.config.Parameters
import chisel3.experimental.Trace._

class TestAggregate extends Bundle {
  val flag = Bool()
  val value = UInt(3.W)
}

class TestAggregate2 extends Bundle {
  val flag = Bool()
  val value = UInt(3.W)
}

class Hello()(implicit p:Parameters) extends LazyModule {
  lazy val  module = new Impl
  class Impl extends LazyModuleImp(this) {

    val io = IO(new Bundle {
      val in = Input(UInt(8.W))
      val out = Output(UInt(3.W))
    })
    val pipe = Module(new Pipe(UInt(8.W), 40))
    val foo = Wire(Vec(8, new TestAggregate))
    val debug_microOp = Reg(Vec(4, new TestAggregate2))
    foo.foreach(ptr => {
      ptr.flag := 0.B
      ptr.value := 0.U
    })
    val index = foo(0).value
    val tt = debug_microOp(index).value
    pipe.io.enq.bits := io.in
    pipe.io.enq.valid := true.B
    io.out := pipe.io.deq.bits
    traceName(tt) // java.lang.NumberFormatException: For input string: "_tt_T"
  }
}

