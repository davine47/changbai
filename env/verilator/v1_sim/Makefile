# Verilator Makefile for V1SimTop
CHANGBAI_ROOT ?= $(abspath ../../..)
RTL_DIR      ?= $(CHANGBAI_ROOT)/rtl
SCALA_DIR    ?= $(CHANGBAI_ROOT)/spinal/src/main/scala/v1/isee

# ---- Bootrom selection ----
BOOTROM ?= bootrom
ifeq ($(BOOTROM),hello)
  BOOTROM_BIN = $(CHANGBAI_ROOT)/sw/nexus-am/apps/hello/build/hello-riscv64-cb-v1.bin
else
  BOOTROM_BIN = $(CHANGBAI_ROOT)/sw/bootrom/bootrom.bin
endif

TOP = V1SimTop

VSRCS  = $(RTL_DIR)/V1SimTop.sv
VSRCS += $(RTL_DIR)/TopV1.sv
VSRCS += $(RTL_DIR)/IseeDecode.sv
VSRCS += $(RTL_DIR)/Frontend.sv
VSRCS += $(RTL_DIR)/RVCDecoder.sv
VSRCS += $(RTL_DIR)/RVCExpander.sv
VSRCS += $(RTL_DIR)/InstQueue.sv
VSRCS += $(RTL_DIR)/StreamFifo.sv
VSRCS += $(RTL_DIR)/Rw64Fetch.sv
VSRCS += $(RTL_DIR)/TestRam.sv
VSRCS += $(RTL_DIR)/ScalarDecode.sv

VFLAGS = --cc --exe --trace -O3 -Wno-fatal --timescale 1ns/1ns
BUILD_DIR = obj_dir
BIN_SRC = $(RTL_DIR)/V1SimTop.sv_toplevel_area_testRam_mem.bin
BIN_DST = V1SimTop.sv_toplevel_area_testRam_mem.bin

all: rtl build run

rtl:
	@echo "[Make] Generating RTL with $(BOOTROM_BIN) ..."
	rm -f $(RTL_DIR)/V1SimTop.sv $(RTL_DIR)/V1SimTop.sv_toplevel_area_testRam_mem.*
	cd $(CHANGBAI_ROOT) && mill -i changbaiV1.spinal.runMain v1.GenV1SimTop "$(BOOTROM_BIN)" 2>&1 | grep "Generated"
	cp $(SCALA_DIR)/IseeDecode.sv $(RTL_DIR)/

$(BIN_DST):
	@if [ -f "$(BIN_SRC)" ]; then cp "$(BIN_SRC)" "$(BIN_DST)"; fi

build: $(BIN_DST)
	@echo "[Make] Verilating $(TOP) ..."
	verilator $(VFLAGS) $(VSRCS) --top-module $(TOP) \
		-CFLAGS "-std=c++17 -O2" \
		isee_dpi.cpp sim_main.cpp -Mdir $(BUILD_DIR)
	$(MAKE) -C $(BUILD_DIR) -f V$(TOP).mk -j4
	@echo "[Make] Build complete"

run:
	./$(BUILD_DIR)/V$(TOP) --max-cycles 5000

wave:
	gtkwave dump.vcd 2>/dev/null || echo "VCD: dump.vcd"

clean:
	rm -rf $(BUILD_DIR) dump.vcd $(BIN_DST)
