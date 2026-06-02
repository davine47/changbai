# TopV1 模块设计文档

> 版本: 1.1 | 文件: `v1/TopV1.scala` | 依赖: `Frontend`, `Rw64Fetch`, `ScalarDecode`

---

## 1. 概述

CPU 顶层模块。集成 Frontend（取指） + Rw64Fetch（协议适配） + ScalarDecode（译码）。对外暴露 RW64 总线（连接 TestRam），输出指令和译码信号。

## 2. 端口

| 信号 | 方向 | 位宽 | 说明 |
|------|------|------|------|
| `io_clk` | in | 1 | 时钟 |
| `io_reset` | in | 1 | 复位 |
| `io_flush` | in | 1 | 清空流水线 |
| `io_instValid` | out | 1 | 指令有效 |
| `io_instBits` | out | 32 | 32-bit 指令 |
| `io_instIsRVC` | out | 1 | 压缩指令标志 |
| `io_instIll` | out | 1 | RVC 非法指令标志 |
| `io_instDecode` | out | ScalarDecodeBundle | 译码信号（19 字段，legal 已含 instIll 门控） |
| `io_rw` | master | Rw64Bus(64,64) | 取指总线 |

## 3. 内部结构

```
io.flush ─────────────────────────┐
io.clk/reset ─────────┐           │
                       ▼           ▼
┌─────────────────────────────────────────┐
│              Frontend                    │
│  toFetch (CpuPipelineBus, master)       │
│  instValid/Bits/IsRVC/Ill              │
└──────────────┬──────────────────────────┘
               │ <>
┌──────────────▼──────────────────────────┐
│           Rw64Fetch                      │
│  rw (Rw64Bus, master) ──► io.rw        │
└─────────────────────────────────────────┘

Frontend.instBits ──► ScalarDecode.inst
ScalarDecode.decode ──► io.instDecode
```

## 4. 译码输出 (ScalarDecodeBundle)

| 信号 | 位宽 | 说明 |
|------|------|------|
| legal | 1 | 合法指令 |
| branch | 1 | 分支指令 |
| jal | 1 | JAL |
| jalr | 1 | JALR |
| rrf1 | 1 | 读 rs1 |
| rrf2 | 1 | 读 rs2 |
| wrf1 | 1 | 写 rd |
| useALU | 1 | 使用 ALU |
| aluOp | 5 | ALU 操作码 |
| useMem | 1 | 访问内存 |
| memOp | 5 | 内存操作码 |
| memResOp | 5 | 内存结果操作 |
| useCsr | 1 | 访问 CSR |
| csrOp | 5 | CSR 操作码 |
| needImmExt | 1 | 需要立即数扩展 |
| immExtType | 3 | 立即数类型编码 |
| fence | 1 | FENCE |
| fenceI | 1 | FENCE.I |
| amo | 1 | 原子操作 |

## 5. 生成命令

```bash
mill -i changbaiV1.spinal.runMain v1.GenTopV1
```

输出: `rtl/TopV1.sv`
