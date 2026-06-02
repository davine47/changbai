// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1.isee

import spinal.core._
import spinal.lib._

// =============================================================================
// IseeDecode — DPI-C blackbox: sends decode events to C backend
//
// Usage: instantiate in DUT, connect to clock + decode outputs.
// Each valid cycle calls the C function difftest_decode() via SystemVerilog DPI.
// =============================================================================

class IseeDecode extends BlackBox {
  val io = new Bundle {
    val clock       = in Bool()
    val valid       = in Bool()
    val pc          = in UInt(64 bits)
    val instruction = in UInt(32 bits)
    val isRVC       = in Bool()
    val ill         = in Bool()
    val legal       = in Bool()
    val aluOp       = in UInt(5 bits)
    val branch      = in Bool()
    val jal         = in Bool()
    val jalr        = in Bool()
    val useMem      = in Bool()
    val memOp       = in UInt(5 bits)
    val useCsr      = in Bool()
    val csrOp       = in UInt(5 bits)
  }

  // DPI-C wrapper is in IseeDecode.sv (added to VSRCS by Makefile)
}
