# Frontend 模块设计文档

> 版本: 2.1 | 文件: `v1/Frontend.scala` | 依赖: `CpuPipelineBus`, `RVCDecoder`, `InstQueue`, `RVCExpander`

---

## 1. 概述

CPU 前端取指模块。自包含自动取指 FSM，直接暴露 CpuPipelineBus。从响应通道读取 64-bit 取指块，经 RVCDecoder → InstQueue → RVCExpander，逐条输出 32-bit 指令。

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
| `io_toFetch` | master | CpuPipelineBus(64,64) | CPU 总线 |

## 3. 内部结构

```
auto-fetch FSM → io.toFetch (CpuPipelineBus, master)
                      ↓
                 respData/respValid
                      ↓
              validReg (RegNext) → 清除 fetchPending
                      ↓
                 RVCDecoder
                      ↓
              carryReg (反馈)
                      ↓
                 InstQueue (深度=64)
                      ↓
                 RVCExpander (组合)
                      ↓
           io.instValid/Bits/IsRVC/Ill
```

## 4. 自动取指 FSM

### 4.1 状态寄存器

| 寄存器 | 说明 |
|--------|------|
| `nextPcReg` | 当前 PC，每指令 +2/+4 |
| `lastFetchAddr` | 上次取指的 8 字节对齐地址 |
| `fetchReq` | 取指请求有效 |
| `fetchPending` | 有取指在途，等待响应 |
| `booted` | 复位后首次取指完成标志 |
| `carryReg` / `hasCarryReg` | 跨 chunk 剩余半字 |

### 4.2 取指触发条件

```scala
needFetch = (hasCarryReg || (fetchAddr > lastFetchAddr)) && !fetchPending
```

- **hasCarryReg**：上一个 chunk 有 straddle，立即取下一连续 chunk
- **fetchAddr > lastFetchAddr**：nextPc 已超过上次取指位置，需要新数据
- **!fetchPending**：没有在途取指，防止 carry 期间重复取指

### 4.3 取指地址

```scala
lastFetchAddr := hasCarryReg ? (lastFetchAddr + 8) : fetchAddr
```

- carry 时取 **lastFetchAddr + 8**（下一连续 chunk）
- 正常时取 **fetchAddr**（nextPcReg 对齐地址）

### 4.4 fetchPending 握手

```
取指请求被接受 (reqReady=1) → fetchPending=1
响应到达 (validReg=1)       → fetchPending=0
```

确保一次只有一笔取指在途。

### 4.5 状态机

| 状态 | 条件 | 行为 |
|------|------|------|
| FLUSH | io.sync.flush=1 | 清零所有状态 |
| BOOT | !booted | 强制取指 addr=0 |
| IDLE | !fetchReq && !needFetch | 等待触发 |
| FETCH | !fetchReq && needFetch | 发起取指 |
| WAIT | fetchReq=1 && reqReady=0 | 等待总线接受 |
| PENDING | fetchPending=1 | 等待响应 |

## 5. 数据路径时序

| 阶段 | 延迟 | 说明 |
|------|------|------|
| FSM → toFetch | 0 | 组合 |
| respValid 捕获 | +1 | RegNext |
| RVCDecoder | 0 | 组合扫描 |
| InstQueue push | 1/条 | Push FSM |
| RVCExpander | 0 | 组合展开 |
| **端到端** | **3-5** | 取指→第一条指令 |

## 6. 生成命令

```bash
mill -i changbaiV1.spinal.runMain v1.GenFrontend
```

输出: `rtl/Frontend.sv`
