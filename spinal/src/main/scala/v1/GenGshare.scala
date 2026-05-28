// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1

import spinal.core.{SpinalConfig, SystemVerilog}

/**
 * Generates standalone Verilog/SystemVerilog for the Gshare branch predictor.
 *
 * Usage:
 *   mill -i changbaiV1.spinal.runMain v1.prediction.GenGshare
 *
 * Output: changbai/rtl/GshareTop.sv
 */
object GenGshare {
  def main(args: Array[String]): Unit = {
    val config = GshareConfig(
      pcWidth = 64,
      historyWidth = 12,
      counterWidth = 2,
      entries = 4096
    )

    // Allow override from command-line with default values
    val pw = if (args.length >= 1) args(0).toInt else config.pcWidth
    val hw = if (args.length >= 2) args(1).toInt else config.historyWidth
    val cw = if (args.length >= 3) args(2).toInt else config.counterWidth
    val en = if (args.length >= 4) args(3).toInt else config.entries

    val finalConfig = config.copy(pcWidth = pw, historyWidth = hw, counterWidth = cw, entries = en)

    SpinalConfig(
      mode = SystemVerilog,
      targetDirectory = "rtl",
      genLineComments = true,
      oneFilePerComponent = true,
      withTimescale = false,
      printFilelist = false
    ).generate {
      new GshareTop(finalConfig)
    }

    println(s"Generated rtl/GshareTop.sv with pcWidth=$pw historyWidth=$hw counterWidth=$cw entries=$en")
  }
}
