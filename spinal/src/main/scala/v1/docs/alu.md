# ALU 模块设计文档

> 版本: 1.1 | 文件: `v1/Alu.scala` | 依赖: `AluConfig`, `AluOp`

---

## 1. 概述

RISC-V RV64IMC 整数算术逻辑单元。实现 RV32I/RV64I 的全部 ALU 操作，包括加/减、移位、逻辑运算、比较、W 后缀操作（32-bit 符号扩展至 64-bit），以及 LUI/AUIPC 立即数通路。

使用 Vec(32) LUT 方法：所有操作结果预计算，`aluOp` 选择输出。纯组合逻辑，零周期延迟。

## 2. 配置

```scala
case class AluConfig(xlen: Int = 64)
```

| 参数 | 默认 | 说明 |
|------|------|------|
| xlen | 64 | 数据位宽（32=RV32，64=RV64） |

## 3. 端口

| 信号 | 方向 | 位宽 | 说明 |
|------|------|------|------|
| `io_src0` | in | xlen | 操作数0（rs1） |
| `io_src1` | in | xlen | 操作数1（rs2 / immediate） |
| `io_aluOp` | in | 5 | ALU 操作码 |
| `io_result` | out | xlen | 计算结果 |

## 4. ALU 操作码编码

`aluOp[4:0]` 编码：

| op[4:0] | 助记符 | 功能 |
|---------|--------|------|
| 00000 | ADD | rd = rs1 + rs2 |
| 01000 | ADDW | rd = sext32(rs1[31:0] + rs2[31:0]) |
| 10000 | SUB | rd = rs1 - rs2 |
| 11000 | SUBW | rd = sext32(rs1[31:0] - rs2[31:0]) |
| 00001 | SLL | rd = rs1 << rs2[5:0] |
| 01001 | SLLW | rd = sext32(rs1[31:0] << rs2[4:0]) |
| 00010 | SLT | rd = signed(rs1) < signed(rs2) ? 1 : 0 |
| 00011 | SLTU | rd = rs1 < rs2 ? 1 : 0 |
| 00100 | XOR | rd = rs1 ^ rs2 |
| 00101 | SRL | rd = rs1 >> rs2[5:0] |
| 01101 | SRLW | rd = sext32(rs1[31:0] >> rs2[4:0]) |
| 10101 | SRA | rd = rs1 >>> rs2[5:0] |
| 11101 | SRAW | rd = sext32(rs1[31:0] >>> rs2[4:0]) |
| 00110 | OR | rd = rs1 | rs2 |
| 00111 | AND | rd = rs1 & rs2 |
| 01010 | LUI | rd = imm（经 src1 传入） |
| 01011 | AUIPC | rd = rs1(PC) + imm |
| 01100 | JAL | rd = rs1 + 4（PC+4） |

编码位域：
- `op[2:0]`：funct3（0=ADD, 1=SLL, 2=SLT, 3=SLTU, 4=XOR, 5=SRL/SRA, 6=OR, 7=AND）
- `op[3]`：isWord（W 后缀 32-bit 操作）
- `op[4]`：isAlt（funct3=000: SUB 替代 ADD, funct3=101: SRA 替代 SRL）

## 5. 内部实现

### 5.1 预计算单元

所有操作结果同时计算：

- **加法器**: `adderOut = src0 + src1`, `subberOut = src0 - src1`
- **32-bit 加法器**: `adderWOut = src0[31:0] + src1[31:0]`
- **左移**: `sllOut = src0 << shamt`（shamt = src1[5:0]）
- **逻辑右移**: `srlOut = src0 >> shamt`
- **算术右移**: `sraOut = src0.asSInt >> shamt`
- **W 移位**: 5-bit shamt，结果 resize 至 32-bit
- **比较**: SLT（有符号）, SLTU（无符号）
- **逻辑**: AND/OR/XOR

### 5.2 结果选择

Vec(32) resultMap，每个条目对应一个 aluOp：

```scala
resultMap(ADD.asUInt)  := adderOut.asBits
resultMap(SUB.asUInt)  := subberOut.asBits
...
result := resultMap(op.asUInt)
```

### 5.3 W 后缀符号扩展

```scala
sext32To64(msb, lo) = Mux(msb, 0xFFFFFFFF, 0x00000000) ## lo
```

## 6. 顶层封装

`AluTop` — 寄存化顶层用于独立 Verilog 生成：

| 信号 | 方向 | 说明 |
|------|------|------|
| `io_clk` | in | 时钟 |
| `io_reset` | in | 复位 |
| `io_src0` | in | 操作数0 |
| `io_src1` | in | 操作数1 |
| `io_aluOp` | in | ALU 操作码 |
| `io_result` | out | 结果 |

## 7. 生成命令

```bash
mill -i changbaiV1.spinal.runMain v1.GenAlu [xlen]
```

输出: `rtl/AluTop.sv`

## 8. 时序

- 纯组合逻辑，零周期延迟
- 关键路径: src0/src1 → 预计算 → MuxOH 选择 → result
- W 后缀符号扩展增加 1 级 Mux 延迟
