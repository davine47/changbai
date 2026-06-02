# TestHarness 模块设计文档

> 版本: 2.1 | 文件: `v1/TestHarness.scala` | 依赖: `TopV1`, `TestRam`, `ScalarDecodeBundle`

---

## 1. 概述

TopV1 + TestRam 集成验证顶层。TopV1 内含 Frontend + Rw64Fetch + ScalarDecode，TestHarness 负责连接 TestRam 并转发指令/译码信号。

## 2. 架构

```
TestHarness
├── TopV1 (Frontend + Rw64Fetch + ScalarDecode)
│   ├── io.rw ──────────► TestRam.rw
│   ├── io.inst* ───────► TestHarness.io.inst*
│   └── io.instDecode ──► TestHarness.io.dec* (带 instValid 门控)
└── TestRam (2048×64bit, bootrom.bin 初始化)
```

> v2.0 变更：不再直接实例化 Frontend/Rw64Fetch/ScalarDecode，改为通过 TopV1 集成。TestRam 使用 SpinalHDL 原生 `mem.initBigInt` 初始化（`$readmemb` + .bin 文件），无需 `patchReadmemh` 后处理。

## 3. 端口

| 信号 | 方向 | 位宽 | 说明 |
|------|------|------|------|
| `io_clk` | in | 1 | 时钟 |
| `io_reset` | in | 1 | 复位 |
| `io_flush` | in | 1 | 清空流水线 |
| `io_instValid` | out | 1 | 指令有效 |
| `io_instBits` | out | 32 | 32-bit 指令 |
| `io_instIsRVC` | out | 1 | 压缩指令标志 |
| `io_instIll` | out | 1 | RVC 非法指令标志 |
| `io_instEffective` | out | 1 | 有效且合法 (instValid && decLegal) |
| `io_decLegal` | out | 1 | 译码合法（含 RVC 非法检测） |
| `io_decBranch` | out | 1 | 分支 |
| `io_decJal` | out | 1 | JAL |
| `io_decJalr` | out | 1 | JALR |
| `io_decRrf1` | out | 1 | 读 rs1 |
| `io_decRrf2` | out | 1 | 读 rs2 |
| `io_decWrf1` | out | 1 | 写 rd |
| `io_decUseALU` | out | 1 | 使用 ALU |
| `io_decAluOp` | out | 5 | ALU 操作码 |
| `io_decUseMem` | out | 1 | 访问内存 |
| `io_decMemOp` | out | 5 | 内存操作码 |
| `io_decMemResOp` | out | 5 | 内存结果操作 |
| `io_decUseCsr` | out | 1 | 访问 CSR |
| `io_decCsrOp` | out | 5 | CSR 操作码 |
| `io_decNeedImmExt` | out | 1 | 需要立即数 |
| `io_decImmExtType` | out | 3 | 立即数类型 |
| `io_decFence` | out | 1 | FENCE |
| `io_decFenceI` | out | 1 | FENCE.I |
| `io_decAmo` | out | 1 | 原子操作 |

所有 `dec*` 信号由 `instValid` 门控：无效时输出 0。

## 4. 地址映射

| 组件 | 地址宽度 | 说明 |
|------|----------|------|
| TopV1 (Rw64Fetch) | 64-bit | 全地址 |
| TestRam | 14-bit (config.addrWidth) | 截断低 14 位（16KB） |

## 5. 配置

```scala
class TestHarness(bootromPath: Option[String] = None)
```

- `bootromPath = None`：空内存
- `bootromPath = Some("sw/bootrom/bootrom.bin")`：从二进制文件初始化

SpinalHDL 的 `mem.initBigInt` 自动生成 `$readmemb` + `.bin` 文件。

## 6. 使用

```bash
# 空内存
mill -i changbaiV1.spinal.runMain v1.GenTestHarness

# 带 bootROM
mill -i changbaiV1.spinal.runMain v1.GenTestHarness sw/bootrom/bootrom.bin
```

输出: `rtl/TestHarness.sv` (+ `TestRam.sv`, `.bin` 文件)

## 7. 验证环境

| 环境 | 目录 |
|------|------|
| cocotb | `env/coco_tb/TestHarness/` |
| Verilator C++ | `env/verilator/TestHarness/` |
