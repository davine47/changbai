#!/usr/bin/env bash
# =============================================================================
# demo_dpi — JNI demo: compile C library + run SpinalSim test
#
# Usage:
#   ./run.sh           — compile & run
#   ./run.sh clean     — clean build artifacts
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHANGBAI_ROOT="$(cd "$SCRIPT_DIR/../../../../../.." && pwd)"

JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home 2>/dev/null || echo /Library/Java/JavaVirtualMachines/jdk-17/Contents/Home)}"
OS="$(uname -s)"

case "$OS" in
    Darwin)
        JNI_PLATFORM="darwin"
        LIB_EXT="dylib"
        ;;
    Linux)
        JNI_PLATFORM="linux"
        LIB_EXT="so"
        ;;
    *)
        echo "Unsupported OS: $OS"
        exit 1
        ;;
esac

JNI_INCLUDE="-I${JAVA_HOME}/include -I${JAVA_HOME}/include/${JNI_PLATFORM}"
LIB_NAME="libdemojni.${LIB_EXT}"
LIB_PATH="${SCRIPT_DIR}/${LIB_NAME}"

cd "$SCRIPT_DIR"

case "${1:-run}" in
    run)
        echo "============================================"
        echo "  demo_dpi — JNI + SpinalSim Demo"
        echo "============================================"

        # Step 1: Compile C native library
        echo ""
        echo "[1/3] Compiling native C library: ${LIB_NAME} ..."
        cc -shared -o "$LIB_NAME" $JNI_INCLUDE demo_jni.c
        echo "       -> ${LIB_PATH}"
        ls -lh "$LIB_PATH"

        # Step 2: Compile Scala
        echo ""
        echo "[2/3] Compiling Scala ..."
        cd "$CHANGBAI_ROOT"
        mill -i changbaiV1.spinal.compile 2>&1 | tail -3

        # Step 3: Run SpinalSim test
        echo ""
        echo "[3/3] Running SpinalSim + JNI test ..."
        cd "$CHANGBAI_ROOT"
        mill -i changbaiV1.spinal.runMain v1.demo_dpi.DemoDpiSim "$LIB_PATH" 2>&1

        echo ""
        echo "============================================"
        echo "  Demo DPI/JNI PASSED"
        echo "============================================"
        ;;

    clean)
        echo "Cleaning ..."
        rm -f "$LIB_NAME"
        echo "Done."
        ;;

    *)
        echo "Usage: $0 [run|clean]"
        exit 1
        ;;
esac
