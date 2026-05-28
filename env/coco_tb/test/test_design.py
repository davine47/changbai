import random
import sys
from pathlib import Path

proj_path = Path(__file__).resolve().parent.parent
sys.path.append(str(proj_path / "ref"))

import cocotb
import cocotb.simulator
from cocotb.triggers import Timer

if cocotb.simulator.is_running():
    from ref_adder import ref_adder

@cocotb.test()
async def adder_basic_test(dut):
    """Test for 5 + 10"""

    A = 5
    B = 10

    dut.io_a.value = A
    dut.io_b.value = B

    await Timer(2, units="ns")

    assert dut.io_sum.value == ref_adder(A, B), f"Adder result is incorrect: {int(dut.io_sum.value)} != 15"

@cocotb.test()
async def adder_randomised_test(dut):
    """Test for adding 2 random numbers multiple times"""

    for i in range(10):
        A = random.randint(0, 2 ** len(dut.io_a) - 1)
        B = random.randint(0, 2 ** len(dut.io_b) - 1)

        dut.io_a.value = A
        dut.io_b.value = B

        await Timer(2, units="ns")

        ref_sum = ref_adder(A, B, len(dut.io_sum.value))

        assert dut.io_sum.value == ref_sum, f"Randomised test failed with:\n  A -> {A} B -> {B}\n  ref -> {ref_sum} dut -> {int(dut.io_sum.value)}"
