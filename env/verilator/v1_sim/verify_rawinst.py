#!/usr/bin/env python3
# verify_rawinst.py — compare DIF rawInst against binary file
# Usage:
#   ./verify_rawinst.py <binary> <dif_output>
#   ./verify_rawinst.py bootrom.bin  (runs sim + captures output automatically)
#   make verify  (via Makefile)
import sys
import os
import re
import subprocess

CHANGBAI_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../.."))

def read_binary(path):
    with open(path, "rb") as f:
        return f.read()

def parse_dif(lines):
    """Parse DIF lines, return list of (pc, raw_inst)."""
    entries = []
    for line in lines:
        m = re.match(r"\[DIF\]\s+\d+\s+\|\s+0x([0-9a-fA-F]+)\s+\|\s+[0-9a-fA-F]+\s+([0-9a-fA-F]+)\s+\|", line)
        if m:
            pc = int(m.group(1), 16)
            raw = int(m.group(2), 16)
            entries.append((pc, raw))
    return entries

def is_rvc(raw):
    """RVC instructions have bits[1:0] != 11."""
    return (raw & 0x3) != 0x3

def expected_from_bin(data, pc, raw):
    """Reconstruct expected 32-bit word from binary at given PC."""
    if is_rvc(raw):
        # RVC: 2 bytes at PC
        if pc + 2 > len(data):
            return None
        return int.from_bytes(data[pc:pc+2], "little")
    else:
        # 32-bit: 4 bytes at PC
        if pc + 4 > len(data):
            return None
        return int.from_bytes(data[pc:pc+4], "little")

def main():
    bin_path = None
    dif_lines = None

    if len(sys.argv) >= 2:
        bin_path = sys.argv[1]

    if len(sys.argv) >= 3:
        # DIF output from file (or stdin with "-")
        if sys.argv[2] == "-":
            dif_lines = sys.stdin.readlines()
        else:
            with open(sys.argv[2]) as f:
                dif_lines = f.readlines()
    else:
        # Run simulation automatically
        if bin_path is None:
            # Try default bootrom
            bin_path = os.path.join(CHANGBAI_ROOT, "sw/bootrom/bootrom.bin")

        sim_dir = os.path.join(CHANGBAI_ROOT, "env/verilator/v1_sim")
        print(f"[verify] Running simulation...")
        result = subprocess.run(
            ["make", "run", f"BOOTROM_BIN={bin_path}"],
            cwd=sim_dir, capture_output=True, text=True
        )
        dif_lines = result.stdout.splitlines()
        if not dif_lines:
            print("[verify] ERROR: no DIF output captured")
            print(result.stderr)
            sys.exit(1)

    data = read_binary(bin_path)
    entries = parse_dif(dif_lines)
    if not entries:
        print("[verify] ERROR: no DIF lines found")
        sys.exit(1)

    mismatches = 0
    compared = 0
    for pc, raw in entries:
        exp = expected_from_bin(data, pc, raw)
        if exp is None:
            if compared > 0:
                print(f"[verify] pc=0x{pc:08x}: out of bounds, stop after {compared} instructions")
            break
        compared += 1
        if exp == raw:
            print(f"[verify] pc=0x{pc:08x}: 0x{raw:08x}  0x{exp:08x}  OK")
        else:
            print(f"[verify] pc=0x{pc:08x}: 0x{raw:08x}  0x{exp:08x}  MISMATCH")
            mismatches += 1

    if mismatches == 0 and compared > 0:
        print(f"[verify] PASS: {compared}/{compared} OK")
    elif compared == 0:
        print(f"[verify] ERROR: no instructions in range")
        sys.exit(1)
    else:
        print(f"[verify] FAIL: {mismatches}/{compared} mismatches")
        sys.exit(1)

if __name__ == "__main__":
    main()
