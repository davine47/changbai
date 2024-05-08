SIM ?= verilator
TOPLEVEL_LANG ?= verilog

VERILOG_SOURCES += $(shell find $(PWD)/../../decode -name "*.v" -or -name "*.sv")

# verilog top module name
TOPLEVEL = DecodingSpecExample

# python test file name
MODULE = test_decoder

EXTRA_ARGS += --trace --trace-structs --timescale 1ns/1ns

include $(shell cocotb-config --makefiles)/Makefile.sim

# dump wave: use $dumpfile and $dumpvars in verilog
