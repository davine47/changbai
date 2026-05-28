import random
import cocotb.simulator
from cocotb.triggers import Timer

@cocotb.test()
async def decode_basic_test(dut):
    A = 0x80180813
    dut.io_rawInst = A
    res = dut.io_result
    await Timer(2, units="ns")
    A = 0x02a80813
    dut.io_rawInst = A
    res = dut.io_result
    await Timer(2, units="ns")