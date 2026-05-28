// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1

import spinal.core._
import spinal.lib._

/**
 * Rw64Fetch configuration.
 *
 * @param addrWidth   address bus width (default 64)
 * @param dataWidth   data bus width (default 64)
 */
case class Rw64FetchConfig(
    addrWidth: Int = 64,
    dataWidth: Int = 64
)

// =============================================================================
// CPU Pipeline Protocol opcode definitions
// =============================================================================
object CpuOpcode {
  val width = 6

  def READ      = B"000000"  // read request
  def WRITE     = B"000001"  // write request
  def REDIRECT   = B"001111"
  // MSG_VALID: upper 2 bits = 11, remaining bits = message type (opcode = 11xxxx)
  def MSG_MASK  = B"110000"  // message operation mask

  def isRead(op: Bits): Bool      = op === READ
  def isWrite(op: Bits): Bool     = op === WRITE
  // Message valid when opcode[5:4] == 11
  def isMsgValid(op: Bits): Bool  = (op & MSG_MASK) === B"110000"
}

// =============================================================================
// CPU Pipeline Protocol length encoding
// =============================================================================
object CpuLen {
  val width = 4

  // len encoding → byte count mapping (reference implementation, unused in this version)
  // 0000: 1B,   0001: 2B,     0010: 4B,     0011: 8B (default)
  // 0100: 16B,  0101: 32B,    0110: 64B,    0111: 128B
  // 1000: 256B, 1001: 512B,   1010: 1024B,  1011: 2048B
  // 1100: 4096B, 1101: 8192B,  1110: 16384B, 1111: 32768B
}

/**
 * RW64 bus — simple read/write interface modeled after Regfile I/O pattern.
 *
 * Write channel:
 *   waddr, wdata, wvalid, wready  — standard valid/ready handshake
 *
 * Read channel:
 *   raddr, rvalid, rready  — read request (valid/ready handshake)
 *   rdata, rresp           — read response (rresp pulses 1 cycle for each response)
 */
class Rw64Bus(val addrWidth: Int, val dataWidth: Int) extends Bundle with IMasterSlave {
  // Write request
  val waddr  = UInt(addrWidth bits)
  val wdata  = Bits(dataWidth bits)
  val wvalid = Bool()
  val wready = Bool()

  // Read request
  val raddr  = UInt(addrWidth bits)
  val rvalid = Bool()
  val rready = Bool()

  // Read response
  val rdata  = Bits(dataWidth bits)
  val rresp  = Bool()  // pulses high when read data is valid

  override def asMaster(): Unit = {
    out(waddr, wdata, wvalid, raddr, rvalid)
    in(wready, rready, rdata, rresp)
  }
}

/**
 * CPU Pipeline Protocol — the interface from CPU pipeline to memory system.
 *
 * Request side:
 *   addr, valid, ready, len, opcode — standard memory request
 *   len: data length encoding (see CpuLen), default 8 bytes (0011)
 *   opcode: operation code (see CpuOpcode), READ=000000, WRITE=000001,
 *           MSG_VALID=11xxxx (upper 2 bits=11, lower 4 bits=msg type)
 *
 * Response side:
 *   valid, ready, data, msg — memory response
 *   msg: reserved, valid when opcode=MSG_VALID
 *
 * Note: write data (reqWdata) is part of the request for store instructions.
 */
case class CpuPipelineBus(addrWidth: Int, dataWidth: Int) extends Bundle with IMasterSlave {
  // Request channel
  val reqAddr   = UInt(addrWidth bits)
  val reqValid  = Bool()
  val reqReady  = Bool()
  val reqLen    = UInt(CpuLen.width bits)
  val reqOpcode = Bits(CpuOpcode.width bits)
  val reqWdata  = Bits(dataWidth bits)  // write data (valid for store ops)

  // Response channel
  val respValid = Bool()
  val respReady = Bool()
  val respData  = Bits(dataWidth bits)
  val respMsg   = Bits(8 bits)

  override def asMaster(): Unit = {
    out(reqAddr, reqValid, reqLen, reqOpcode, reqWdata, respReady)
    in(reqReady, respValid, respData, respMsg)
  }
}

/**
 * Rw64Fetch — CPU pipeline request to RW64 protocol adapter.
 *
 * Translates CPU memory access requests into RW64 read/write bus transactions.
 * Supports three operation types defined by opcode:
 *
 *   READ  (000000): issue raddr on RW64 bus, collect rdata → CPU resp
 *   WRITE (000001): issue waddr+wdata on RW64 bus, write completes on wready
 *   MSG_VALID (111111): pass-through msg without data transfer
 *
 * The len field encodes the data length in bytes (see CpuLen encoding).
 * In current version, len is fixed at 8 bytes (0011) — single-beat 64-bit transfer.
 * Future versions may use len to generate multi-beat RW64 transactions.
 *
 * Read path latency: 1 cycle (CPU req → RW64 raddr → rresp → CPU resp)
 * Write path latency: 0 cycles handshake (CPU req fires when wready=1)
 *
 * Pure combinational logic — no internal registers.
 */
class Rw64Fetch(config: Rw64FetchConfig) extends Component {
  import config._
  import CpuOpcode._

  val io = new Bundle {
    val cpu = slave(CpuPipelineBus(addrWidth, dataWidth))
    val rw  = master(new Rw64Bus(addrWidth, dataWidth))
  }

  // =========================================================================
  // Opcode decode (full 6-bit)
  // =========================================================================
  val isReadCmd  = CpuOpcode.isRead(io.cpu.reqOpcode)
  val isWriteCmd = CpuOpcode.isWrite(io.cpu.reqOpcode)
  val isMsgCmd   = CpuOpcode.isMsgValid(io.cpu.reqOpcode)

  // =========================================================================
  // Length field (reserved for future multi-beat support)
  // =========================================================================
  // io.cpu.reqLen carries the byte count encoding per CpuLen table.
  // Current version: single-beat 64-bit transfers only.

  // =========================================================================
  // RW64 Write channel
  // =========================================================================
  io.rw.waddr  := io.cpu.reqAddr
  io.rw.wdata  := io.cpu.reqWdata
  io.rw.wvalid := io.cpu.reqValid && isWriteCmd

  // =========================================================================
  // RW64 Read channel
  // =========================================================================
  io.rw.raddr  := io.cpu.reqAddr
  io.rw.rvalid := io.cpu.reqValid && isReadCmd

  // =========================================================================
  // CPU Request ready
  // =========================================================================
  // READ:  ready when RW64 read request channel accepts
  // WRITE: ready when RW64 write channel accepts
  // MSG_VALID: always ready (no bus transaction needed)
  io.cpu.reqReady := Mux(
    isWriteCmd,
    io.rw.wready,                     // write: wait for wready
    Mux(isReadCmd, io.rw.rready, True)   // read: wait for rready; msg: always ready
  )

  // =========================================================================
  // CPU Response
  // =========================================================================
  // READ:  driven by RW64 read response
  // WRITE: no response (write completed on handshake)
  // MSG_VALID: no data, msg passed through as-is
  io.cpu.respValid := Mux(
    isMsgCmd,
    io.cpu.reqValid,    // msg_valid: response immediately with msg
    Mux(isReadCmd, io.rw.rresp, False)  // read: wait for rresp; write: no response
  )
  io.cpu.respData  := Mux(isReadCmd, io.rw.rdata, B(0, dataWidth bits))
  // MSG_VALID: respMsg = {4'b0, opcode[3:0]} — lower 4 bits = message type
  io.cpu.respMsg   := Mux(isMsgCmd, B"0000" ## io.cpu.reqOpcode(3 downto 0), B(0, 8 bits))
}

// =============================================================================
// Top-level wrapper + Generator
// =============================================================================

/**
 * Top-level wrapper for standalone Verilog generation with flat I/O.
 */
class Rw64FetchTop(config: Rw64FetchConfig = Rw64FetchConfig()) extends Component {
  import config._

  val io = new Bundle {
    val clk   = in Bool()
    val reset = in Bool()

    // CPU request
    val cpu_reqAddr   = in UInt(addrWidth bits)
    val cpu_reqValid  = in Bool()
    val cpu_reqReady  = out Bool()
    val cpu_reqLen    = in UInt(CpuLen.width bits)
    val cpu_reqOpcode = in Bits(CpuOpcode.width bits)
    val cpu_reqWdata  = in Bits(dataWidth bits)

    // CPU response
    val cpu_respValid = out Bool()
    val cpu_respReady = in Bool()
    val cpu_respData  = out Bits(dataWidth bits)
    val cpu_respMsg   = out Bits(8 bits)

    // RW64 write channel
    val rw_waddr   = out UInt(addrWidth bits)
    val rw_wdata   = out Bits(dataWidth bits)
    val rw_wvalid  = out Bool()
    val rw_wready  = in Bool()

    // RW64 read request channel
    val rw_raddr   = out UInt(addrWidth bits)
    val rw_rvalid  = out Bool()
    val rw_rready  = in Bool()

    // RW64 read response channel
    val rw_rdata   = in Bits(dataWidth bits)
    val rw_rresp   = in Bool()
  }

  val cd = ClockDomain(clock = io.clk, reset = io.reset)
  val area = new ClockingArea(cd) {
    val fetch = new Rw64Fetch(config)

    fetch.io.cpu.reqAddr   := io.cpu_reqAddr
    fetch.io.cpu.reqValid  := io.cpu_reqValid
    io.cpu_reqReady        := fetch.io.cpu.reqReady
    fetch.io.cpu.reqLen    := io.cpu_reqLen
    fetch.io.cpu.reqOpcode := io.cpu_reqOpcode
    fetch.io.cpu.reqWdata  := io.cpu_reqWdata

    io.cpu_respValid := fetch.io.cpu.respValid
    fetch.io.cpu.respReady := io.cpu_respReady
    io.cpu_respData  := fetch.io.cpu.respData
    io.cpu_respMsg   := fetch.io.cpu.respMsg

    io.rw_waddr  := fetch.io.rw.waddr
    io.rw_wdata  := fetch.io.rw.wdata
    io.rw_wvalid := fetch.io.rw.wvalid
    fetch.io.rw.wready := io.rw_wready

    io.rw_raddr  := fetch.io.rw.raddr
    io.rw_rvalid := fetch.io.rw.rvalid
    fetch.io.rw.rready := io.rw_rready

    fetch.io.rw.rdata := io.rw_rdata
    fetch.io.rw.rresp := io.rw_rresp
  }
}

/**
 * Generates standalone Verilog/SystemVerilog for the Rw64Fetch module.
 *
 * Usage:
 *   mill -i changbaiV1.spinal.runMain v1.rw64fetch.GenRw64Fetch
 *
 * Output: changbai/rtl/Rw64FetchTop.sv
 */
object GenRw64Fetch {
  def main(args: Array[String]): Unit = {
    val config = Rw64FetchConfig(
      addrWidth = if (args.length >= 1) args(0).toInt else 64,
      dataWidth = if (args.length >= 2) args(1).toInt else 64
    )

    SpinalConfig(
      mode = SystemVerilog,
      targetDirectory = "rtl",
      genLineComments = true,
      oneFilePerComponent = true,
      withTimescale = false,
      printFilelist = false
    ).generate {
      new Rw64FetchTop(config)
    }

    println(s"Generated rtl/Rw64FetchTop.sv (addr=${config.addrWidth}, data=${config.dataWidth})")
  }
}
