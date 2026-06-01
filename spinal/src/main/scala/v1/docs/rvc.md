# RVC 指令子系统设计文档

> 版本: 1.2 | 文件: `v1/RVCDecoder.scala`, `v1/RVCExpander.scala`, `v1/InstQueue.scala`

---

## 1. 架构概览

```
fetchData[63:0]
     │
     ▼
┌─────────────┐    ┌──────────────┐    ┌────────────┐
│ RVCDecoder  │───►│  InstQueue   │───►│ RVCExpander│
│ (组合)      │    │ (深度=16)    │    │ (组合)     │
│ 指令边界扫描│    │ 4→1 条/周期  │    │ 16→32 bit │
└─────────────┘    └──────────────┘    └────────────┘
```

三个模块协同工作，将 64-bit 取指块转换为逐条 32-bit 指令流。

---

## 2. RVCDecoder — 指令边界扫描器

### 2.1 功能

在 64-bit 取指块中扫描最多 4 条指令的边界。支持 16-bit 和 32-bit 混合，支持跨 64-bit 块边界的 32-bit 指令（carry 机制）。

### 2.2 端口

| 信号 | 方向 | 位宽 | 说明 |
|------|------|------|------|
| `io_fetchData` | in | 64 | 取指数据 |
| `io_valid` | in | 1 | 数据有效门控 |
| `io_carryIn` | in | 16 | 上一块的残留半字 |
| `io_hasCarryIn` | in | 1 | 有残留半字 |

| `io_instCount` | out | 3 | 有效指令数 |
| `io_inst0Valid` | out | 1 | slot0 有效 |
| `io_inst0Is32` | out | 1 | slot0=32-bit |
| `io_inst1Valid` | out | 1 | slot1 有效 |
| `io_inst1Is32` | out | 1 | slot1=32-bit |
| `io_inst2Valid` | out | 1 | slot2 有效 |
| `io_inst2Is32` | out | 1 | slot2=32-bit |
| `io_inst3Valid` | out | 1 | slot3 有效 |
| `io_inst3Is32` | out | 1 | slot3=32-bit |

| `io_carryOut` | out | 16 | 跨块残留半字 |
| `io_hasCarryOut` | out | 1 | 有跨块残留 |

### 2.3 扫描算法

取 5 个半字位置（slot）：

```
Byte offset:  0  1  2  3  4  5  6  7
             [hw0  ][hw1  ][hw2  ][hw3  ]
                  [carryIn (if any)]
```

每个位置检测指令类型（inst[1:0]）：

| inst[1:0] | 类型 | 长度 |
|-----------|------|------|
| 00, 01, 10 | 压缩指令 | 16-bit |
| 11 | 32-bit 指令 | 32-bit |

**扫描流程**（从 slot0 开始）：
1. hasCarryIn ? → 第一条是 32-bit（carryIn ## hw0），跳到 slot1
2. slot0 的 hw0[1:0] = 11 ? → 32-bit（hw0 ## hw1），跳到 slot2
3. slot0 的 hw0[1:0] ≠ 11 ? → 16-bit（仅 hw0），跳到 slot1
4. 继续扫描下一个 slot...

**跨块检测**：
- 32-bit 指令的第二个半字超出块边界（byte 6-7 作为前半，后半在下一块）
- hasCarryOut=1 → 下半字存入 carryOut 供下一块使用

### 2.4 门控

`io_valid = 0` 时所有输出为 0。

### 2.5 生成命令

```bash
mill -i changbaiV1.spinal.runMain v1.GenRVCDecoder
```

---

## 3. RVCExpander — 压缩指令展开器

### 3.1 功能

将 16-bit RISC-V 压缩指令展开为等价的 32-bit 指令。**纯组合逻辑**（`always @(*)`，无 clk/reset）。

### 3.2 端口

| 信号 | 方向 | 位宽 | 说明 |
|------|------|------|------|
| `io_instIn` | in | 16 | 16-bit 压缩指令 |
| `io_instOut_bits` | out | 32 | 展开的 32-bit 指令 |
| `io_instOut_ill` | out | 1 | 非法编码 |
| `io_rvc` | out | 1 | 输入为合法压缩指令 |

### 3.3 opIdx 编码

`opIdx = {inst[1:0], inst[15:13]}`，6-bit，共 32 个编码，其中 18 个有效：

| opIdx | 象限 | funct3 | 指令 |
|-------|------|--------|------|
| 0 | C0(00) | 000 | C.ADDI4SPN |
| 2 | C0(00) | 010 | C.LW |
| 3 | C0(00) | 011 | C.LD |
| 6 | C0(00) | 110 | C.SW |
| 7 | C0(00) | 111 | C.SD |
| 8 | C1(01) | 000 | C.ADDI / C.NOP |
| 9 | C1(01) | 001 | C.ADDIW |
| 10 | C1(01) | 010 | C.LI |
| 11 | C1(01) | 011 | C.LUI / C.ADDI16SP |
| 12 | C1(01) | 100 | C.SRLI/SRAI/ANDI(funct2=0,1,2) / C.SUB/XOR/OR/AND/SUBW/ADDW(funct2=3) |
| 13 | C1(01) | 101 | C.J |
| 14 | C1(01) | 110 | C.BEQZ |
| 15 | C1(01) | 111 | C.BNEZ |
| 16 | C2(10) | 000 | C.SLLI |
| 18 | C2(10) | 010 | C.LWSP |
| 19 | C2(10) | 011 | C.LDSP |
| 20 | C2(10) | 100 | C.JR/MV/JALR/ADD |
| 22 | C2(10) | 110 | C.SWSP |
| 23 | C2(10) | 111 | C.SDSP |

opIdx=1,4,5,17,21 为非法/保留编码，输出 `ill=1`。

> C.FLD/C.FLW/C.FSW/C.FLDSP/C.FLWSP（浮点压缩指令）和 C.EBREAK（opIdx=20 的特殊情形）在当前整数实现中标记为非法。

### 3.4 实现方式

```scala
switch(opIdx) {
  is(U(N)) { /* 对应象限的展开逻辑 */ }
  ...
  default { instOut.bits := 0; instOut.ill := True }
}
```

位域重映射规则参考 RISC-V 压缩指令集手册（C 扩展章节）。

### 3.5 非法编码

`ill = True` 当：
- opIdx 不在 24 个有效象限中
- 保留编码（如 C.LUI 的 rd=0 或 imm=0）

### 3.6 生成命令

```bash
mill -i changbaiV1.spinal.runMain v1.GenRVCExpander
```

---

## 4. InstQueue — 指令缓冲队列

### 4.1 功能

将 RVCDecoder 的 0~4 条/周期输出缓冲为稳定的 1 条/周期输出。支持 flush 清空，支持跨块恢复。

### 4.2 端口

| 信号 | 方向 | 位宽 | 说明 |
|------|------|------|------|
| `io_flush` | in | 1 | 清空所有状态 |
| `io_fetchData` | in | 64 | 取指块数据（传入 StreamFifo） |
| `io_carryIn` | in | 16 | 上一块残留半字 |
| `io_hasCarryIn` | in | 1 | 有残留 |
| `io_inst0/1/2/3Valid` | in | 1 | slot 有效 |
| `io_inst0/1/2/3Is32` | in | 1 | slot=32-bit |

| `io_instValid` | out | 1 | 指令有效 |
| `io_instBits` | out | 32 | 指令（32-bit 完整，16-bit 仅低16位） |
| `io_isRVC` | out | 1 | 原始为压缩指令 |

### 4.3 内部结构

```
inst0-3 → [4-entry Buffer] → [Push FSM] → [StreamFifo(depth=16)] → 输出
```

1. **Buffer**: 4 个 entry，每周期锁存 inst0-3 的 Valid/Is32 和对应 fetchData 片段
2. **Push FSM**: 将 buffer 中的有效指令依次推入 StreamFifo
   - 第一级: 构建 33-bit payload = `{instBits[31:0], isRVC}`
   - 第二级: pushIdx 状态机，逐条推入
3. **StreamFifo**: 标准 ready/valid FIFO，深度 16

### 4.4 关键特性

- **flush**: 清空 buffer + pushIdx 复位 + StreamFifo flush
- **allEmpty 检测**: pushFsm 在全部空时复位 pushIdx，防止残留导致乱序
- **输出功耗**: 1 条/周期，标准 ready/valid handshake

### 4.5 生成命令

```bash
mill -i changbaiV1.spinal.runMain v1.GenInstQueue
```
