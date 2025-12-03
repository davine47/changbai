import random
import cocotb.simulator
from cocotb.triggers import Timer

@cocotb.test()
async def decode_basic_test(dut):
    A = 0b100
    dut.sel = A
    res = dut.result
    print(res)
    await Timer(2, units="ns")
    A = 0b011
    dut.sel = A
    res = dut.result
    print(res)
    await Timer(2, units="ns")