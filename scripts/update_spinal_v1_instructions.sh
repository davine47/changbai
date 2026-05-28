#!/bin/bash

cd $CHANGBAI_ROOT/sw/riscv-opcodes
ls $CHANGBAI_ROOT/sw/riscv-opcodes/extensions
make EXTENSIONS='rv*_i rv*_m rv*_a rv*_f rv*_d rv*_c rv_v'
gsed -i '1i import spinal.core.LiteralBuilder' inst.spinalhdl
gsed -i '1i package v1' inst.spinalhdl
cp inst.spinalhdl $CHANGBAI_ROOT/spinal/src/main/scala/v1/Instructions.scala
