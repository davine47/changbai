import random
import cocotb.simulator
from cocotb.clock import Clock
from cocotb.triggers import Timer

@cocotb.test()
async def play_basic_test(dut):
    dut.a_0.value = 0
    dut.a_1.value = 1
    dut.a_2.value = 2
    dut.a_3.value = 3
    dut.a_4.value = 4
    dut.a_5.value = 5
    dut.a_6.value = 6
    dut.a_7.value = 7
    dut.mask.value = 0b11100000
    await Timer(2, units="ns")
    dut.mask.value = 0b00000001
    await Timer(2, units="ns")
    dut.mask.value = 0b10010110
    await Timer(2, units="ns")
    dut.mask.value = 0b00000000
    await Timer(2, units="ns")
    dut.mask.value = 0b11111111
    await Timer(2, units="ns")