def ref_adder(a: int, b: int, data_width: int = 64) -> int:
    return (a + b) & (2 ** data_width - 1)
