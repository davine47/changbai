#!/usr/bin/env bash
# run.sh — Generate RTL + build + run DifftestDemoTop
# Usage:
#   ./run.sh              → bootrom
#   ./run.sh hello        → hello world
#   ./run.sh bootrom      → bootrom (explicit)
#   ./run.sh wave         → gtkwave
#   ./run.sh clean        → cleanup
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

PROG="${1:-bootrom}"

case "$PROG" in
    bootrom|hello)
        echo "============================================"
        echo "  DifftestDemoTop — $PROG"
        echo "============================================"
        make BOOTROM="$PROG"
        ;;
    wave)
        make wave
        ;;
    clean)
        make clean
        ;;
    *)
        echo "Usage: $0 [bootrom|hello|wave|clean]"
        exit 1
        ;;
esac
