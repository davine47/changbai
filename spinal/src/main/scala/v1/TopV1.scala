package v1

import spinal.core._
import spinal.lib._
import v1.testram.{TestRam, TestRamConfig}

// =============================================================================
// TopV1 — CPU top level: Frontend + Rw64Fetch
//
// Exposes RW64 bus for external TestRam connection.
// =============================================================================

class TopV1(bootromPath: Option[String] = None) extends Component {
  val io = new Bundle {
    val clk = in Bool()
    val reset = in Bool()

    // === RW64 fetch interface (connect to TestRam) ===
    val rw = master(new Rw64Bus(64, 64))

    // === instruction output ===
    val instValid = out Bool()
    val instBits  = out Bits(32 bits)
    val instIsRVC = out Bool()
    val instIll   = out Bool()
    val instDecode = out(new ScalarDecodeBundle)

    // === control ===
    val flush = in Bool()
  }
  val coreRootClockDomain = ClockDomain(clock = io.clk, reset = io.reset)

  // =====================================================================
  // Fetch Interface
  // =====================================================================
  val area = new ClockingArea(coreRootClockDomain) {
    val rw64 = new Rw64Fetch(Rw64FetchConfig(addrWidth = 64, dataWidth = 64))

    // Manual pass-through (both are master, same direction)
    io.rw.waddr  := rw64.io.rw.waddr
    io.rw.wdata  := rw64.io.rw.wdata
    io.rw.wvalid := rw64.io.rw.wvalid
    rw64.io.rw.wready := io.rw.wready
    io.rw.raddr  := rw64.io.rw.raddr
    io.rw.rvalid := rw64.io.rw.rvalid
    rw64.io.rw.rready := io.rw.rready
    rw64.io.rw.rdata  := io.rw.rdata
    rw64.io.rw.rresp  := io.rw.rresp

    val frontend = new Frontend

    rw64.io.cpu <> frontend.io.toFetch

    frontend.io.sync.flush := io.flush
    frontend.io.clk := io.clk
    frontend.io.reset := io.reset

    io.instValid := frontend.io.instValid
    io.instBits := frontend.io.instBits
    io.instIsRVC := frontend.io.instIsRVC
    io.instIll := frontend.io.instIll

    val decode = new ScalarDecode

    decode.io.inst    := frontend.io.instBits
    decode.io.instIll := frontend.io.instIll
    io.instDecode <> decode.io.decode

  }

}
