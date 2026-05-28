# Frontend 模块设计文档

> 版本: 2.0 | 文件: `v1/Frontend.scala` | 依赖: `CpuPipelineBus`, `RVCDecoder`, `InstQueue`, `RVCExpander`

---

## 1. 概述

CPU 前端取指模块。自包含自动取指 FSM，直接暴露 CpuPipelineBus（不再内部实例化 Rw64Fetch）。从 CpuPipelineBus 响应通道读取 64-bit 取指块，经 RVCDecoder 边界扫描 → InstQueue 缓冲 → RVCExpander 展开，逐条输出 32-bit 指令。

## 2. 端口

| 信号 | 方向 | 位宽 | 说明 |
|------|------|------|------|
| `io_clk` | in | 1 | 时钟 |
| `io_reset` | in | 1 | 复位 |
| `io_instValid` | out | 1 | 指令有效 |
| `io_instBits` | out | 32 | 32-bit 指令（展开后） |
| `io_instIsRVC` | out | 1 | 原始为 16-bit 压缩指令 |
| `io_instIll` | out | 1 | RVC 非法指令标志 |
| `io_sync_flush` | in | 1 | 清空流水线 + PC 归零 |
| `io_toFetch` | master | CpuPipelineBus(64,64) | CPU 总线（对外连 Rw64Fetch） |

> v2.0 变更：`io_isRVC` → `io_instIsRVC`，`io_rvcIll` → `io_instIll`，`io_flush` → `io.sync.flush`，移除 `io_nextPc` 和 `io_rw`（Rw64Bus）。Frontend 不再内部包含 Rw64Fetch。

## 3. 内部结构

```
auto-fetch FSM → io.toFetch (CpuPipelineBus, master)
                      ↓
                 respData/respValid
                      ↓
              validReg (RegNext)
                      ↓
                 RVCDecoder
                      ↓
              carryReg (反馈)
                      ↓
                 InstQueue (深度=16)
                      ↓
                 RVCExpander (组合)
                      ↓
           io.instValid/Bits/IsRVC/Ill
```

## 4. 自动取指 FSM

### 4.1 状态

| 状态 | 条件 | 行为 |
|------|------|------|
| FLUSH | io.sync.flush=1 | 清零所有状态 |
| BOOT | !booted (复位后) | 强制取指 addr=0 |
| IDLE | !fetchReq | 等待 needFetch 触发 |
| FETCH | fetchReq=1 | 等待 io.toFetch.reqReady |

### 4.2 触发条件

```scala
needFetch = (fetchAddr ≠ lastFetchAddr) || hasCarryReg
```

- **fetchAddr**: `nextPcReg[63:3] @@ "000"` — 8 字节对齐
- **跨块**: hasCarryReg=1 时立即取下一块（解决 straddle 死锁）

### 4.3 CPU 请求配置

```scala
io.toFetch.reqLen    := 3   // 8 bytes
io.toFetch.reqOpcode := CpuOpcode.READ
io.toFetch.reqWdata  := 0
io.toFetch.respReady := True
```

固定发出 8 字节读请求。

## 5. 数据路径时序

| 阶段 | 延迟 | 说明 |
|------|------|------|
| FSM → toFetch | 0 | 组合 |
| respValid 捕获 | +1 | RegNext |
| RVCDecoder | 0 | 组合扫描 |
| InstQueue | 1~4 | Buffer + Push FSM |
| RVCExpander | 0 | 组合展开 |
| **端到端** | **3-5** | 取指→第一条指令 |

## 6. 生成命令

```bash
mill -i changbaiV1.spinal.runMain v1.GenFrontend
```

输出: `rtl/Frontend.sv`
