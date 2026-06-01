# Regfile 模块设计文档

> 版本: 1.2 | 文件: `v1/Regfile.scala` | 依赖: `RegfileConfig`

---

## 1. 概述

RISC-V 寄存器文件。32 个架构寄存器（x0-x31），x0 硬连线为 0。支持可配置的读写端口数（默认 2R1W），用于超标量设计。

实现方式：Vec(Reg) 触发器阵列。

## 2. 配置

```scala
case class RegfileConfig(
    readPorts: Int  = 2,    // 读端口数
    writePorts: Int = 1,    // 写端口数
    xlen: Int       = 64,   // 位宽
    numRegs: Int    = 32    // 寄存器数量（32 或 16）
)
```

| 约束 | 值 |
|------|-----|
| 最小读端口 | 1 |
| 最小写端口 | 1 |
| 有效寄存器数 | 32 (RV32I/RV64I) 或 16 (RV32E) |
| 有效位宽 | 32 或 64 |

## 3. 端口

| 信号 | 方向 | 位宽 | 说明 |
|------|------|------|------|
| `io_readAddr[N]` | in × readPorts | log2(numRegs) | 读地址 |
| `io_readData[N]` | out × readPorts | xlen | 读数据 |
| `io_writeAddr[N]` | in × writePorts | log2(numRegs) | 写地址 |
| `io_writeData[N]` | in × writePorts | xlen | 写数据 |
| `io_writeEn[N]` | in × writePorts | 1 | 写使能 |

## 4. 读写行为

### 读

- 组合逻辑（无延迟）：`readData = regs[readAddr]`
- x0 永远返回 0

### 写

- 时序逻辑：`writeEn` 为高时，时钟上升沿更新寄存器
- 多端口写同一寄存器时，高索引端口优先（last-write-wins）
- 写 x0 被静默忽略

### 写前读（Read-After-Write）

同一周期写和读：读端口看到的是旧值（寄存器在时钟沿更新）。

## 5. 内部实现

```scala
val regs = Vec(Reg(Bits(xlen bits)) init 0, numRegs)

// 读: 组合选择
for (i <- 0 until readPorts) {
  io.readData(i) := regs(io.readAddr(i))
}
when(io.readAddr(i) === 0) { io.readData(i) := 0 }

// 写: x0 保护 + 多端口仲裁
for (i <- 0 until writePorts) {
  for (j <- 0 until numRegs) {
    when(io.writeEn(i) && io.writeAddr(i) === j && j =/= 0) {
      regs(j) := io.writeData(i)
    }
  }
}
```

## 6. 顶层封装

`RegfileTop` — 寄存化顶层：

| 信号 | 方向 | 说明 |
|------|------|------|
| `io_clk` | in | 时钟 |
| `io_reset` | in | 复位 |
| `io_readAddr_0/1` | in | 读地址 |
| `io_readData_0/1` | out | 读数据 |
| `io_writeAddr` | in | 写地址 |
| `io_writeData` | in | 写数据 |
| `io_writeEn` | in | 写使能 |

## 7. 生成命令

```bash
mill -i changbaiV1.spinal.runMain v1.GenRegfile [readPorts] [writePorts] [xlen] [numRegs]
```

输出: `rtl/RegfileTop.sv`

## 8. 时序

| 路径 | 延迟 |
|------|------|
| 读地址 → 读数据 | 组合（~MuxOH 延迟） |
| 写数据 → 寄存器更新 | 1 周期（时钟沿） |
| 寄存器 → 读数据 | 组合 |

## 9. 面积

- Vec(Reg): 32 × 64 = 2048 FFs（2R1W 配置）
- 组合读 Mux: 32:1 × 2 端口
