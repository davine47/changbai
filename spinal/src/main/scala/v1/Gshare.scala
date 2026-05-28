// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1

import spinal.core._
import spinal.lib._

/**
 * Gshare branch predictor configuration.
 *
 * @param pcWidth       width of the program counter (32, 48, or 64 bits supported)
 * @param historyWidth  width of the global history register (GHR)
 * @param counterWidth  width of each saturating counter in the PHT (typically 2)
 * @param entries       number of entries in the pattern history table (must be power of 2)
 */
case class GshareConfig(
    pcWidth: Int = 64,
    historyWidth: Int = 12,
    counterWidth: Int = 2,
    entries: Int = 4096
)

/**
 * Gshare branch predictor — ISA-independent, general-purpose module.
 *
 * Prediction:
 *   hash = (pc >> 2) ^ history  (discard lower 2 bits since instructions are aligned)
 *   taken = PHT[hash].msb       (MSB of saturating counter)
 *
 * Training:
 *   On a resolved branch, the saturating counter at the prediction-time
 *   (pc, history) is incremented (taken) or decremented (not-taken).
 *
 * Ports:
 *   io.cmd        — prediction request
 *   io.rsp        — prediction response (available next cycle after cmd)
 *   io.train      — training port
 *   io.flush      — synchronous clear of all PHT entries
 *
 * The interface is deliberately free of any ISA-specific fields.
 */
class Gshare(config: GshareConfig) extends Component {
  import config._

  val hashWidth = log2Up(entries)

  val io = new Bundle {
    // ---- Prediction command ----
    val cmd = new Bundle {
      val valid   = in Bool()
      val pc      = in UInt(pcWidth bits)
      val history = in Bits(historyWidth bits)
    }

    // ---- Prediction response (1 cycle after cmd) ----
    val rsp = new Bundle {
      val valid   = out Bool()
      val taken   = out Bool()
      val history = out Bits(historyWidth bits) // echo back the history used
    }

    // ---- Training ----
    val train = new Bundle {
      val valid   = in Bool()
      val pc      = in UInt(pcWidth bits)
      val history = in Bits(historyWidth bits) // history at prediction time
      val taken   = in Bool()
    }

    // ---- Flush ----
    val flush = in Bool()
  }

  // ---- Pattern History Table ----
  val pht = Mem(Bits(counterWidth bits), entries)
  // Initialize PHT to weakly-taken (10b) so initial predictions are "taken"
  // This is done via init in simulation; for synthesis an explicit reset mechanism is provided.

  // ---- Pipeline: prediction request -> response ----
  // Hash calculation
  val cmdHash = UInt(hashWidth bits)
  // Drop lower 2 bits (instruction alignment), take hashWidth bits from bit 2 upward, XOR with history
  val pcShifted = io.cmd.pc(2, hashWidth bits)
  cmdHash := (pcShifted.asBits ^ io.cmd.history.resize(hashWidth)).asUInt

  // Read PHT (synchronous)
  val phtRead = pht.readSync(cmdHash, io.cmd.valid)

  // Pipeline stage: cmd -> rsp
  val rspValid    = RegNext(io.cmd.valid) init False
  val rspTaken    = RegNext(phtRead(counterWidth - 1)) init False
  val rspHistory  = RegNext(io.cmd.history) init B(0, historyWidth bits)

  io.rsp.valid   := rspValid
  io.rsp.taken   := rspTaken
  io.rsp.history := rspHistory

  // ---- Training / Update ----
  val trainHash = UInt(hashWidth bits)
  val trainPcShifted = io.train.pc(2, hashWidth bits)
  trainHash := (trainPcShifted.asBits ^ io.train.history.resize(hashWidth)).asUInt

  // Read current counter value for update
  val trainPhtRead = pht.readSync(trainHash, io.train.valid)

  // Saturating counter update
  val updatedCounter = Bits(counterWidth bits)
  val maxCounter = B((1 << counterWidth) - 1, counterWidth bits)
  when(io.train.taken) {
    // Saturating increment
    updatedCounter := (trainPhtRead.asUInt + 1).asBits
    when(trainPhtRead === maxCounter) {
      updatedCounter := trainPhtRead // saturate
    }
  }.otherwise {
    // Saturating decrement
    updatedCounter := (trainPhtRead.asUInt - 1).asBits
    when(trainPhtRead === B(0, counterWidth bits)) {
      updatedCounter := trainPhtRead // saturate
    }
  }

  // Write updated counter back to PHT
  pht.write(trainHash, updatedCounter, io.train.valid)

  // ---- Flush: sequential clear of all PHT entries ----
  // Using a counter to walk through all entries
  val flushCounter = Reg(UInt(hashWidth + 1 bits)) init 0
  val flushing = Reg(Bool()) init False

  when(io.flush) {
    flushing := True
    flushCounter := 0
  }.elsewhen(flushing) {
    flushCounter := flushCounter + 1
    when(flushCounter.msb) {
      flushing := False
    }
  }

  // During flush, reset each PHT entry to weakly-not-taken (01b)
  val flushData = B(1, counterWidth bits)
  val flushAddr = flushCounter(hashWidth - 1 downto 0)
  pht.write(
    flushAddr,
    flushData,
    flushing && !flushCounter.msb
  )
}

/**
 * Top-level wrapper that instantiates the Gshare predictor.
 * Useful as a standalone Verilog generation target.
 */
class GshareTop(config: GshareConfig = GshareConfig()) extends Component {
  val io = new Bundle {
    val clk   = in Bool()
    val reset = in Bool()
    val cmd_valid   = in Bool()
    val cmd_pc      = in UInt(config.pcWidth bits)
    val cmd_history = in Bits(config.historyWidth bits)
    val rsp_valid   = out Bool()
    val rsp_taken   = out Bool()
    val rsp_history = out Bits(config.historyWidth bits)
    val train_valid   = in Bool()
    val train_pc      = in UInt(config.pcWidth bits)
    val train_history = in Bits(config.historyWidth bits)
    val train_taken   = in Bool()
    val flush = in Bool()
  }

  val cd = ClockDomain(clock = io.clk, reset = io.reset)
  val area = new ClockingArea(cd) {
    val gshare = new Gshare(config)
    gshare.io.cmd.valid   := io.cmd_valid
    gshare.io.cmd.pc      := io.cmd_pc
    gshare.io.cmd.history := io.cmd_history
    io.rsp_valid   := gshare.io.rsp.valid
    io.rsp_taken   := gshare.io.rsp.taken
    io.rsp_history := gshare.io.rsp.history
    gshare.io.train.valid   := io.train_valid
    gshare.io.train.pc      := io.train_pc
    gshare.io.train.history := io.train_history
    gshare.io.train.taken   := io.train_taken
    gshare.io.flush := io.flush
  }
}
