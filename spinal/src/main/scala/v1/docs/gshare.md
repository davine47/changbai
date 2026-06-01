# Gshare 分支预测器设计文档

> 版本: 1.2 | 文件: `v1/Gshare.scala`, `v1/GenGshare.scala` | ISA-independent

---

## 1. 概述

Gshare 全局历史分支预测器。使用 PC 与全局历史寄存器（GHR）异或作为 PHT 索引。2-bit 饱和计数器。

ISA 无关设计——不依赖任何 ISA 特定字段（无跳转偏移、无立即数等）。

## 2. 配置

```scala
case class GshareConfig(
    pcWidth: Int       = 64,    // PC 位宽
    historyWidth: Int  = 12,    // GHR 位宽
    counterWidth: Int  = 2,     // 计数器位宽（通常 2）
    entries: Int       = 4096   // PHT 条目数（2 的幂）
)
```

## 3. 端口

### 预测命令 (cmd)

| 信号 | 方向 | 位宽 | 说明 |
|------|------|------|------|
| `cmd_valid` | in | 1 | 预测请求有效 |
| `cmd_pc` | in | pcWidth | 取指 PC |
| `cmd_history` | in | historyWidth | 当前 GHR |

### 预测响应 (rsp) — 下一拍

| 信号 | 方向 | 位宽 | 说明 |
|------|------|------|------|
| `rsp_valid` | out | 1 | 响应有效 |
| `rsp_taken` | out | 1 | 预测跳转 |
| `rsp_history` | out | historyWidth | 使用的 GHR（回显） |

### 训练端口 (train)

| 信号 | 方向 | 位宽 | 说明 |
|------|------|------|------|
| `train_valid` | in | 1 | 训练请求有效 |
| `train_pc` | in | pcWidth | 分支 PC（预测时） |
| `train_history` | in | historyWidth | 预测时的 GHR |
| `train_taken` | in | 1 | 实际跳转结果 |

### 控制

| 信号 | 方向 | 位宽 | 说明 |
|------|------|------|------|
| `io_flush` | in | 1 | 同步清零 PHT |

## 4. 算法

### 预测

```
hash = (pc >> 2) ^ zeroExtend(history, hashWidth)
taken = PHT[hash].msb   // 饱和计数器最高位
```

- `pc >> 2`: 丢弃低 2 位（指令对齐，忽略最低两位）
- history 0-extend 到 hashWidth（处理 historyWidth < hashWidth）

### 训练

```
hash = (prediction_pc >> 2) ^ zeroExtend(prediction_history, hashWidth)
if taken: PHT[hash] = min(PHT[hash] + 1, 2^counterWidth - 1)
else:     PHT[hash] = max(PHT[hash] - 1, 0)
```

### 计数器语义（2-bit 示例）

| 值 | 含义 |
|----|------|
| 10, 11 | 强/弱预测跳转 |
| 00, 01 | 强/弱预测不跳转 |

## 5. 内部实现

```scala
val pht = Vec(Reg(UInt(counterWidth bits)) init initVal, entries)
```

- Vec(Reg) 实现
- 预测：组合读（readAsync）
- 训练：时序写（时钟沿）

## 6. 顶层封装

`GshareTop` — 寄存化顶层：

| 时钟/复位 | `io_clk`, `io_reset` |
| 命令/响应/训练 | `io_cmd_*`, `io_rsp_*`, `io_train_*` |
| flush | `io_flush` |

## 7. 生成命令

```bash
mill -i changbaiV1.spinal.runMain v1.GenGshare [pcWidth] [historyWidth] [counterWidth] [entries]
```

输出: `rtl/GshareTop.sv`

## 8. 时序

| 路径 | 延迟 |
|------|------|
| cmd → rsp | 1 周期（RegNext 寄存器） |
| train → PHT 更新 | 1 周期（时钟沿） |
| flush → PHT 清零 | 1 周期（同步） |
