package v1.regfile

import spinal.core.{B, Bits, Component, IntToBuilder, Mem, UInt, Vec, Verilator, in, out}
import spinal.lib.experimental.chisel.Bundle


class SimpleRegFile extends Component{

  val io = new Bundle {
    val raddr0 = in UInt(5 bits)
    val rdata0 = out UInt(64 bits)
    val raddr1 = in UInt(5 bits)
    val rdata1 = out UInt(64 bits)
    val wen = in Bool()
    val waddr = in UInt(5 bits)
    val wdata = in UInt(64 bits)
  }

  val regFile = Mem(Bits(64 bits), 32) addAttribute(Verilator.public)

  regFile.init(List.fill(32)(B(0, 32 bits)))
  io.rdata0 := regFile.readSync(io.raddr0).asUInt
  io.rdata1 := regFile.readSync(io.raddr1).asUInt
  regFile.write(io.waddr, io.wdata.asBits, io.wen)
}
