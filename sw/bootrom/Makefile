# AI[DeepSeek V4 Pro, high] GENERATED BEGIN
# BootROM compilation for TestRam verification
#
# Uses RISC-V GNU toolchain to compile bootrom.S → bootrom.bin/elf/img/hex
# AI GENERATED END

RISCV_PREFIX ?= $(HOME)/riscv/bin/riscv64-unknown-elf-

GCC = $(RISCV_PREFIX)gcc
OBJCOPY = $(RISCV_PREFIX)objcopy

BOOTROM = bootrom
HEX_DEPTH = 2048
HEX_WIDTH = 8  # 8 bytes = 64-bit per TestRam word

all: $(BOOTROM).img

%.img: %.bin
	dd if=$< of=$@ bs=128 count=1

%.bin: %.elf
	$(OBJCOPY) -O binary $< $@

%.elf: %.S linker.ld
	$(GCC) -Tlinker.ld $< -nostdlib -static -Wl,--no-gc-sections -o $@

# AI[DeepSeek V4 Pro, high] GENERATED BEGIN
# hex target: convert bootrom.bin to TestRam .hex format
# Format: $(HEX_DEPTH) lines of $(HEX_WIDTH)*2 hex chars (64-bit little-endian words)
hex: $(BOOTROM).bin bin2hex.py
	@python3 bin2hex.py $(BOOTROM).bin $(BOOTROM).hex $(HEX_WIDTH) $(HEX_DEPTH)
# AI GENERATED END

clean:
	rm -f $(BOOTROM).img $(BOOTROM).bin $(BOOTROM).elf $(BOOTROM).hex
