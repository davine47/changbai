// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
package v1.demo_dpi

// =============================================================================
// DemoJniBridge — Scala ↔ C JNI bridge (minimal demo)
//
// Usage:
//   1. Compile C library: see demo_jni.c header comments
//   2. Run DemoDpiSim which calls compute/isLegal during SpinalSim
//
// JNI name mapping:
//   Java_v1_demo_1dpi_DemoJniBridge_compute
//   └─ Java_ ──┘ └── package with _ for . ──┘ └ class ┘ └ method ┘
//   v1.demo_dpi → v1_demo_1dpi
// =============================================================================

object DemoJniBridge {
  // Load the native shared library (libdemojni.dylib or libdemojni.so)
  // The library must be on java.library.path or absolute path
  private var loaded = false

  def init(libPath: Option[String] = None): Unit = {
    if (!loaded) {
      libPath match {
        case Some(path) => System.load(path)          // absolute path
        case None       => System.loadLibrary("demojni") // from java.library.path
      }
      loaded = true
      println(s"[JNI Scala] Native library loaded successfully")
    }
  }

  @native def compute(a: Int, b: Int): Int
  @native def isLegal(inst: Int): Int
}
