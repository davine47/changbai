# CSR 模块设计文档

> 版本: 1.1 | 文件: `v1/CSR.scala` | 依赖: `CSRs` 地址常量

---

## 1. 概述

RISC-V 特权架构 CSR（Control and Status Register）模块。实现 18 个 Machine 模式 CSR 寄存器，支持读写访问，非法地址检测。

## 2. CSR 地址常量

```scala
object CSRs {
  // Machine Information (RO)
  Mvendorid=0xF11, Marchid=0xF12, Mimpid=0xF13, Mhartid=0xF14, Mconfigptr=0xF15
  // Machine Trap Setup
  Mstatus=0x300, Misa=0x301, Medeleg=0x302, Mideleg=0x303, Mie=0x304, Mtvec=0x305, Mcounteren=0x306
  // Machine Trap Handling
  Mscratch=0x340, Mepc=0x341, Mcause=0x342, Mtval=0x343, Mip=0x344
}
```

## 3. 端口

### 命令接口 (cmd)

| 信号 | 方向 | 位宽 | 说明 |
|------|------|------|------|
| `cmd_valid` | in | 1 | 命令有效 |
| `cmd_addr` | in | 12 | CSR 地址 |
| `cmd_wdata` | in | 64 | 写数据 |
| `cmd_wen` | in | 1 | 写使能 |

### 响应接口 (rsp)

| 信号 | 方向 | 位宽 | 说明 |
|------|------|------|------|
| `rsp_rdata` | out | 64 | 读数据 |
| `rsp_valid` | out | 1 | 响应有效（下一拍） |
| `rsp_illegal` | out | 1 | 非法访问 |

## 4. 支持的 CSR 列表

| CSR | 地址 | RW | 复位值 | 说明 |
|-----|------|-----|--------|------|
| mvendorid | 0xF11 | RO | 0 | Vendor ID |
| marchid | 0xF12 | RO | 0 | Architecture ID |
| mimpid | 0xF13 | RO | 0 | Implementation ID |
| mhartid | 0xF14 | RO | 0 | Hardware thread ID |
| mconfigptr | 0xF15 | RO | 0 | Configuration pointer |
| mstatus | 0x300 | RW | 0x1800 | Machine status (FS=01) |
| misa | 0x301 | RW | 0x80000000_0014012D | ISA (RV64IMCS) |
| medeleg | 0x302 | RW | 0 | Machine exception delegation |
| mideleg | 0x303 | RW | 0 | Machine interrupt delegation |
| mie | 0x304 | RW | 0 | Machine interrupt enable |
| mtvec | 0x305 | RW | 0x100 | Machine trap vector |
| mcounteren | 0x306 | RW | 7 | Machine counter enable |
| mscratch | 0x340 | RW | 0 | Machine scratch |
| mepc | 0x341 | RW | 0 | Machine exception PC |
| mcause | 0x342 | RW | 0 | Machine cause |
| mtval | 0x343 | RW | 0 | Machine trap value |
| mip | 0x344 | RW | 0 | Machine interrupt pending |

## 5. 访问行为

| 条件 | rdata | illegal | 说明 |
|------|-------|---------|------|
| 已实现地址 + 读 | 当前值 | 0 | 正常读 |
| 已实现地址 + 写 (RW) | - | 0 | 正常写 |
| 已实现地址 + 写 (RO) | 原值 | 1 | RO 只读 |
| 未实现地址 | 0 | 1 | 非法访问 |

valid 在下一拍返回（RegNext 寄存器）。

## 6. 内部实现

- 18 个 64-bit `Reg` 寄存器
- 组合逻辑译码 `cmd_addr` → 选择对应寄存器
- 读: 组合 Mux 选择输出
- 写: `cmd_wen && !isReadOnly` 时更新寄存器
- `rsp_valid = RegNext(cmd_valid)`

## 7. 顶层封装

`CSRTop` — 寄存化顶层：

| 信号 | 方向 | 说明 |
|------|------|------|
| `io_clk` | in | 时钟 |
| `io_reset` | in | 复位 |
| `io_cmd_*` | in | 命令 |
| `io_rsp_*` | out | 响应 |

## 8. 生成命令

```bash
mill -i changbaiV1.spinal.runMain v1.GenCSR
```

输出: `rtl/CSRTop.sv`

## 9. 时序

- 读: 1 周期延迟（cmd_valid → rsp_valid 下一拍）
- 写: 同周期生效（下一拍可读）
- 纯寄存器实现，无 SRAM
