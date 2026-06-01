# ScalarDecode 模块设计文档

> 版本: 2.1 | 文件: `v1/ScalarDecode.scala` | 依赖: `Instructions._`, `utils.DecodeConst`

---

## 1. 概述

RISC-V RV64IMC 标量指令译码器。将 32-bit 指令转换为 19 个功能信号。基于 `AbstractDecodeSigs` 框架，表驱动译码。纯组合逻辑。

## 2. 译码输出信号 (ScalarDecodeBundle)

19 字段，共 37 bits：

| 信号 | 位宽 | 说明 |
|------|------|------|
| legal | 1 | 合法指令（0=非法） |
| branch | 1 | 分支指令 (BEQ/BNE/BLT/BGE/BLTU/BGEU) |
| jal | 1 | JAL |
| jalr | 1 | JALR |
| rrf1 | 1 | 读 rs1 |
| rrf2 | 1 | 读 rs2 |
| wrf1 | 1 | 写 rd |
| useALU | 1 | 使用 ALU |
| aluOp | 5 | ALU 操作码（见 AluOp 编码） |
| useMem | 1 | 访问内存 |
| memOp | 5 | 内存操作码（LB/LH/LW/LD/SB/SH/SW/SD） |
| memResOp | 5 | 内存结果操作（符号/零扩展） |
| useCsr | 1 | 访问 CSR |
| csrOp | 5 | CSR 操作码（RW/RS/RC/RWI/RSI/RCI） |
| needImmExt | 1 | 需要立即数扩展 |
| immExtType | 3 | 立即数类型（I/S/SB/U/UJ/Z） |
| fence | 1 | FENCE |
| fenceI | 1 | FENCE.I |
| amo | 1 | 原子操作 |

> v2.0 变更：从旧版 12-bit 编码升级为完整的 19 字段 Bundle。

## 3. 端口

| 信号 | 方向 | 位宽 | 说明 |
|------|------|------|------|
| `io_inst` | in | 32 | 指令（已展开至 32-bit） |
| `io_instIll` | in | 1 | RVC 非法标志，强制 legal=0 |
| `io_decode` | out | ScalarDecodeBundle | 19 字段译码信号 |

## 4. 译码表

覆盖 RV64IMC 全部指令，按 opcode + funct3/funct7 区分：

| 类别 | 指令示例 |
|------|---------|
| ALU R-type | ADD/SUB/SLL/SLT/SLTU/XOR/SRL/SRA/AND/OR + W 变体 |
| ALU I-type | ADDI/SLLI/SLTI/SLTIU/XORI/SRLI/SRAI/ORI/ANDI + W 变体 |
| LUI/AUIPC | LUI, AUIPC |
| Branch | BEQ/BNE/BLT/BGE/BLTU/BGEU |
| Load | LB/LH/LW/LD/LBU/LHU/LWU |
| Store | SB/SH/SW/SD |
| JAL/JALR | JAL, JALR |
| CSR | CSRRW/CSRRS/CSRRC/CSRRWI/CSRRSI/CSRRCI |
| FENCE | FENCE, FENCE.I |
| AMO | AMO* (catch-all) |
| SYSTEM | ECALL/EBREAK/MRET/WFI |

## 5. 合法指令检测

`legal` 信号由 `Symplify.logicOf` 计算，再经 `io.instIll` 门控：
```scala
when(io.instIll) { io.decode.legal := False }
```

## 6. 时序

- 纯组合逻辑（无 clk/reset）
- 顶层封装 `ScalarDecodeTop` 提供寄存化时钟域

## 7. 生成命令

```bash
mill -i changbaiV1.spinal.runMain v1.GenScalarDecode
```

输出: `rtl/ScalarDecodeTop.sv`
