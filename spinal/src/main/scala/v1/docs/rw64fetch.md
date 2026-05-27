# Rw64Fetch 模块设计文档（含 Rw64Bus 协议）

> 版本: 1.1 | 文件: `v1/Rw64Fetch.scala` | 依赖: `Rw64Bus`, `CpuPipelineBus`, `CpuOpcode`, `CpuLen`

---

## 1. 概述

Rw64Fetch 将 CPU 流水线的内存访问请求转换为 RW64 总线事务。支持读、写、消息传递三种操作。纯组合逻辑。

在 TopV1 中，Frontend 的 `io.toFetch`（CpuPipelineBus）通过 Rw64Fetch 转换为 `io.rw`（Rw64Bus）连接外部 TestRam。

Rw64Bus 是一个简洁的读写总线接口，仿照 Regfile I/O 模式设计，包含独立的读/写通道和单个周期的读响应脉冲。

## 2. CpuPipelineBus 协议

CPU 流水线使用的请求/响应协议：

### 请求通道

| 信号 | 位宽 | 说明 |
|------|------|------|
| `reqAddr` | addrWidth | 请求地址（字节寻址） |
| `reqValid` | 1 | 请求有效 |
| `reqReady` | 1 | Frontend 可接受 |
| `reqLen` | 4 | 数据长度编码（见 CpuLen） |
| `reqOpcode` | 6 | 操作码 |
| `reqWdata` | dataWidth | 写数据（store 指令时有效） |

### 响应通道

| 信号 | 位宽 | 说明 |
|------|------|------|
| `respValid` | 1 | 响应有效 |
| `respReady` | 1 | 下游可接受 |
| `respData` | dataWidth | 读数据 |
| `respMsg` | 8 | 消息内容（MSG_VALID 时有效） |

### CpuOpcode 编码

| 值 | 助记符 | 说明 |
|----|--------|------|
| 000000 | READ | 读请求 |
| 000001 | WRITE | 写请求 |
| 11xxxx | MSG_VALID | 消息（高两位=11，低4位=消息类型） |

### CpuLen 编码（2^len 字节）

| len | 字节 | len | 字节 |
|-----|------|-----|------|
| 0000 | 1B | 0100 | 16B |
| 0001 | 2B | 0101 | 32B |
| 0010 | 4B | 0110 | 64B |
| 0011 | 8B | 0111 | 128B |

> 当前版本固定 len=0011（8 字节），单拍 64-bit 传输。

## 3. Rw64Bus 协议

### 写通道

| 信号 | 方向 (master) | 位宽 | 说明 |
|------|---------------|------|------|
| `waddr` | out | addrWidth | 写地址 |
| `wdata` | out | dataWidth | 写数据 |
| `wvalid` | out | 1 | 写有效 |
| `wready` | in | 1 | 写就绪 |

### 读请求通道

| 信号 | 方向 (master) | 位宽 | 说明 |
|------|---------------|------|------|
| `raddr` | out | addrWidth | 读地址 |
| `rvalid` | out | 1 | 读有效 |
| `rready` | in | 1 | 读就绪 |

### 读响应

| 信号 | 方向 (master) | 位宽 | 说明 |
|------|---------------|------|------|
| `rdata` | in | dataWidth | 读数据 |
| `rresp` | in | 1 | 读响应脉冲（单周期） |

### 读事务时序

```
T0: raddr=X, rvalid=1, rready=1  → 请求被接受
T1: rdata=Y, rresp=1              → 单拍响应脉冲
```

> `rresp` 是单周期脉冲，下一个时钟沿即清零。需用 `RegNext` 寄存。

### IMasterSlave 定义

```scala
class Rw64Bus(addrWidth, dataWidth) extends Bundle with IMasterSlave {
  override def asMaster(): Unit = {
    out(waddr, wdata, wvalid, raddr, rvalid)
    in(wready, rready, rdata, rresp)
  }
}
```

## 4. Rw64Fetch 内部逻辑（纯组合）

### 读路径

```
isRead  → rw.raddr = cpu.reqAddr
        → rw.rvalid = cpu.reqValid && isRead
        → cpu.reqReady = rw.rready
        → cpu.respValid = rw.rresp
        → cpu.respData = rw.rdata
```

### 写路径

```
isWrite → rw.waddr = cpu.reqAddr
        → rw.wdata = cpu.reqWdata
        → rw.wvalid = cpu.reqValid && isWrite
        → cpu.reqReady = rw.wready
        → cpu.respValid = False（写无响应）
```

### 消息路径

```
isMsg   → cpu.reqReady = True（立即接受）
        → cpu.respValid = cpu.reqValid
        → cpu.respMsg = {4'b0, opcode[3:0]}
        → 无总线事务
```

## 5. 配置

```scala
case class Rw64FetchConfig(addrWidth: Int = 64, dataWidth: Int = 64)
```

## 6. 顶层封装

`Rw64FetchTop` — 寄存化顶层：

| 时钟/复位 | `io_clk`, `io_reset` |
| CPU 请求 | `io_cpu_reqAddr/Valid/Ready/Len/Opcode/Wdata` |
| CPU 响应 | `io_cpu_respValid/Ready/Data/Msg` |
| RW64 总线 | `io_rw_*` |

## 7. 生成命令

```bash
mill -i changbaiV1.spinal.runMain v1.GenRw64Fetch [addrWidth] [dataWidth]
```

输出: `rtl/Rw64FetchTop.sv`

## 8. 时序

| 路径 | 延迟 |
|------|------|
| CPU req → RW64 bus | 0 周期（组合） |
| rresp → CPU resp | 0 周期（组合） |
| 读总延迟 | 1 周期（req → bus → mem → rresp） |
| 写延迟 | 0 周期（wready 握手） |
