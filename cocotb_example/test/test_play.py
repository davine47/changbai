import random
import cocotb.simulator
from cocotb.clock import Clock
from cocotb.triggers import Timer

@cocotb.test()
async def play_basic_test(dut):
    dut.a.value = 0b10001000
    await Timer(2, units="ns")
    dut.a.value = 0b00000001
    await Timer(2, units="ns")
    dut.a.value = 0b11111111
    await Timer(2, units="ns")