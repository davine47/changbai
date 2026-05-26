# AI[DeepSeek V4 Pro, high] GENERATED BEGIN
# Convert binary to TestRam .hex format
# Usage: python3 bin2hex.py <input.bin> [output.hex] [width=8] [depth=2048]
# AI GENERATED END

import sys

binpath = sys.argv[1]
out     = sys.argv[2] if len(sys.argv) > 2 else binpath.replace('.bin', '.hex')
width   = int(sys.argv[3]) if len(sys.argv) > 3 else 8
depth   = int(sys.argv[4]) if len(sys.argv) > 4 else 2048

with open(binpath, 'rb') as f:
    data = f.read()

pad_len = depth * width - len(data)
padded  = data + b'\x00' * pad_len

with open(out, 'w') as f:
    for i in range(0, len(padded), width):
        word = int.from_bytes(padded[i:i+width], 'little')
        f.write(f'{word:0{width*2}x}\n')

print(f'[bin2hex] {binpath} -> {out} ({depth} words)')
