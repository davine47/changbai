SIM ?= verilator
TOPLEVEL_LANG ?= verilog

VERILOG_SOURCES += $(shell find $(PWD)/../../adder -name "*.v" -or -name "*.sv")

# verilog top module name
TOPLEVEL = EnvAdder

# python test file name
MODULE = test_design

include $(shell cocotb-config --makefiles)/Makefile.sim

# dump wave: use $dumpfile and $dumpvars in verilog
