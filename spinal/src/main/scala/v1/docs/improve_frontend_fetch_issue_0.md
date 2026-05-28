# improve_frontend_fetch_issue_0: InstQueue Push FSM 吞吐优化

> 日期: 2026-05-27 | 影响模块: `v1/InstQueue.scala`

---

## 1. 问题描述

Frontend 取指密度不高，同一 64-bit chunk 内的指令间隔为 2 拍（正常应为 1 拍）。测试观察：

```
修复前 (50 instr, 136 cycles):
Cycle  6: 指令 (32-bit)
Cycle  8: 指令 (32-bit)    +2 ← chunk 内
Cycle 16: 指令 (RVC)       +8 ← chunk 间
Cycle 18: 指令 (32-bit)    +2
Cycle 20: 指令 (RVC)       +2

修复后 (50 instr, 110 cycles, ~20% 提升):
Cycle  5: 指令 (32-bit)
Cycle  6: 指令 (32-bit)    +1 ← 改善
Cycle 13: 指令 (RVC)       +7
Cycle 14: 指令 (32-bit)    +1
Cycle 15: 指令 (RVC)       +1
```

---

## 2. 根因分析

### 2.1 数据路径

```
RVCDecoder → Staging Buffer (4 entries) → Push FSM → StreamFifo → Output
```

RVCDecoder 在一个周期内输出最多 4 条指令的边界信息（`inst0Valid`~`inst3Valid`）。这些信息被 4 个 staging 寄存器（`bufValid[0:3]`）同时捕获。然后 Push FSM 逐条推入 StreamFifo。

### 2.2 旧 Push FSM 设计

```scala
val pushing = Reg(Bool()) init False

fifoIn.valid := pushing                    // 仅当 pushing=1 时有效三
fifoIn.payload := bufBits(pushIdx) ## ...

when(!pushing) {
  when(bufValid(pushIdx)) { pushing := True }    // 拍1: 检测 → 设置 pushing
}.otherwise {
  when(fifoIn.fire) {                             // 拍2: FIFO 接受 → 清除
    bufValid(pushIdx) := False
    pushing := False
    pushIdx := pushIdx + 1
  }
}
```

每条指令推送需 2 拍：

| 拍 | 动作 |
|----|------|
| N | 检测 `bufValid[idx]=1`，设置 `pushing←1`。但 `fifoIn.valid` 此时仍为旧值 0 |
| N+1 | `pushing=1` → `fifoIn.valid=1`。FIFO 接受 → `fire=1` → 清除 `bufValid[idx]`，`pushing←0` |

根因：`pushing` 寄存器在检测与推送之间引入了 1 拍延迟。`fifoIn.fire` 依赖 `valid && ready`，而 `valid` 由 `pushing` 驱动，形成两拍握手。

---

## 3. 修复方案

去掉 `pushing` 中间寄存器，改为直接由 `bufValid` 和 FIFO `ready` 驱动：

```scala
// 旧: 两拍握手
fifoIn.valid := pushing
when(!pushing) {
  when(bufValid(pushIdx)) { pushing := True }
}.otherwise {
  when(fifoIn.fire) { bufValid(pushIdx) := False; pushing := False; pushIdx++ }
}

// 新: 一拍直推
fifoIn.valid := bufValid(pushIdx) && fifo.io.push.ready
fifoIn.payload := bufBits(pushIdx) ## bufRvc(pushIdx).asBits

when(bufValid(pushIdx) && fifo.io.push.ready) {
  bufValid(pushIdx) := False
  pushIdx := pushIdx + 1
}
```

### 3.1 时序对比

**修复前（2 拍/条）：**

```
拍N:   bufValid[0] 置位, pushing=0
       → !pushing, bufValid[0]=1 → pushing←1
       → fifoIn.valid 仍为 0（旧 pushing）

拍N+1: pushing=1 → fifoIn.valid=1
       → FIFO ready → fire → bufValid[0]←0, pushing←0, pushIdx←1

拍N+2: bufValid[1] 为 1, pushing=0
       → !pushing, bufValid[1]=1 → pushing←1
       → 同拍N, fifoIn.valid 仍为 0
```

**修复后（1 拍/条）：**

```
拍N:   bufValid[0] 置位
       → bufValid[0]=1 && fifoReady=1 → fifoIn.valid=1
       → FIFO 同拍接受 → fire → bufValid[0]←0, pushIdx←1

拍N+1: bufValid[1]=1 && fifoReady=1 → fifoIn.valid=1
       → FIFO 同拍接受 → fire → bufValid[1]←0, pushIdx←2
```

### 3.2 正确性保证

原设计的 `pushing` 寄存器提供了"正在推送"状态保护，防止同一 `bufValid` 条目被重复推送。新设计中，`bufValid(pushIdx) := False` 在推送接受的同一拍清除，等价保护。

FIFO `ready` 信号保证推送不会在 FIFO 满时丢失数据——与旧设计中 `fifoIn.fire` 的握手语义一致。

---

## 4. 影响范围

| 项目 | 状态 |
|------|------|
| InstQueue.scala | push FSM 重写（删除 `pushing` 寄存器） |
| Frontend.scala | 无变更 |
| 其他模块 | 无影响 |
| cocotb 测试 (TestHarness) | 4/4 PASS |
| Verilator 测试 (TestHarness) | 100 指令 decode 0 失败 |
| 取指密度 | chunk 内 +2→+1 拍，总周期约 -20% |

---

## 5. 未解决问题（后续优化）

chunk 间气泡（~7 拍）仍存在，原因是 Frontend FSM 的串行取指策略：

```
取指请求 → 等响应(3拍) → 解码 → 队列出队 → PC 跨边界 → 下一轮取指
```

FSM 需要在 chunk 完全消费后才发起下一次取指（`needFetch` 依赖 `nextPcReg` 跨 8 字节边界）。改为预取策略（收到响应后立即取下一 chunk）可消除此气泡。
