import random
import sys
from pathlib import Path

proj_path = Path(__file__).resolve().parent.parent
sys.path.append(str(proj_path))

import cocotb
import cocotb.simulator
from cocotb.triggers import Timer
from cocotb.triggers import RisingEdge
from cocotb.clock import Clock

if cocotb.simulator.is_running():
    from ref.ref_adder import ref_adder

@cocotb.test()
async def adder_randomised_test(dut):
    """Test for adding 2 random numbers multiple times"""

    for _ in range(10):
        A = random.randint(0, 2 ** len(dut.u_adder.src1))
        B = random.randint(0, 2 ** len(dut.u_adder.src2))

        dut.src1.value = A
        dut.src2.value = B

        await Timer(2, units="ns")

        ref_sum = ref_adder(A, B, len(dut.u_adder.sum))

        assert_str = (
            f"Randomsize test failed with:\n"
            f"A -> {A} B -> {B}\n"
            f"ref -> {ref_sum:0{len(dut.u_adder.sum)}b}\n"
            f"dut -> {dut.u_adder.sum.value}\n"
        )
        assert dut.u_adder.sum.value == ref_sum, assert_str

@cocotb.test()
async def reg_test(dut):
    """Test Reg with Synchronized Asynchronous Reset"""

    clk = Clock(dut.clk, 2, "ns")
    await cocotb.start(clk.start())

    # reset dut
    dut.rst.value = 1
    for _ in range(3):
        await RisingEdge(dut.clk)
    dut.rst.value = 0

    # run simulation
    for _ in range(20):
        A = random.randint(0, 2 ** len(dut.u_adder.src1))
        B = random.randint(0, 2 ** len(dut.u_adder.src2))

        dut.src1.value = A
        dut.src2.value = B

        await RisingEdge(dut.clk)
