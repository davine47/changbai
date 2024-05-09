import random
import cocotb.simulator
from cocotb.clock import Clock
from cocotb.triggers import Timer

@cocotb.test()
async def regfile_basic_test(dut):
    cocotb.fork(Clock(dut.clk, 1000).start())
    dut.io_wen.value = 0b1
    dut.io_waddr.value = 0x1
    dut.io_wdata.value = 0x5050
    dut.io_raddr0.value = 0x1
    await Timer(2, units="ns")
    dut.io_wen.value = 0b1
    dut.io_waddr.value = 0x2
    dut.io_wdata.value = 0x5252
    await Timer(2, units="ns")
    dut.io_wen.value = 0b1
    dut.io_waddr.value = 0x2
    dut.io_wdata.value = 0x5252
    await Timer(2, units="ns")