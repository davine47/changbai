# fix_frontend_carry_deadlock_bug_0: Frontend FSM carry 链死锁与 bufValid 覆盖丢数据

> 日期: 2026-05-29 | 影响模块: `v1/Frontend.scala`, `v1/InstQueue.scala`

---

## 1. 问题现象

用 5.5KB 的 hello 程序（`sw/nexus-am/apps/hello/build/hello-riscv64-cb-v1.bin`）作为 bootrom 测试 TestHarness 时，Frontend 取指到第 35 条指令后卡死：

```
修复前:
  Instructions: 35, IPC: 0.05, Density: 5%

修复后:
  Instructions: 325, IPC: 0.22, Density: 22%
```

---

## 2. 根因分析

### 2.1 Bug 1：`needFetch` 不等号导致 carry 后重复取旧地址

**旧代码：**

```scala
val needFetch = (fetchAddr =/= lastFetchAddr) || hasCarryReg

when(needFetch) {
  fetchReq := True
  lastFetchAddr := fetchAddr  // ← 总是用 fetchAddr
}
```

**时序问题：**

程序中的 `fmv.w.x` 指令序列导致每 64-bit chunk 都产生 carry（straddle）。carry 链将 `lastFetchAddr` 快速推进到远离 `nextPcReg` 的地址：

```
FSM 状态快照 (卡死点):
  nextPcReg     = 0x8a     (PC 在原地)
  lastFetchAddr = 0xc0     (carry 链已超前取到 0xc0)
  hasCarryReg  = 1         (上一个 chunk 有 straddle)
  needFetch    = 1
```

当 carry 链最终遇到无 straddle 的 chunk（`hasCarryReg=0`）：

```
needFetch = (fetchAddr != lastFetchAddr) || 0
          = (0x88 != 0xc0) || 0
          = 1
```

FSM 重新取指，但 `lastFetchAddr := fetchAddr = 0x88`——取的是**早已取过的旧块**。由于 `nextPcReg` 远小于 `lastFetchAddr`，取回来的指令无法推进 PC，FSM 在重复取旧地址和超前取指之间死循环。

**修复：**

```scala
// 只有 nextPc 真正超过上次取指地址时才触发
val needFetch = hasCarryReg || (fetchAddr > lastFetchAddr)

// carry 时取下一块 (lastFetchAddr+8)，否则取正常对齐地址
lastFetchAddr := hasCarryReg ? (lastFetchAddr + 8) | fetchAddr
```

### 2.2 Bug 2：`bufValid` 被连续 carry 响应覆盖导致指令丢失

旧代码中，`hasCarryReg` 的更新不受 InstQueue 缓冲状态控制：

```scala
when(validReg) {
  carryReg    := decoder.io.carryOut
  hasCarryReg := decoder.io.hasCarryOut
}
```

**丢数据时序：**

carry 链连续触发取指，每个响应通过 `validReg` 更新 `hasCarryReg`。解码器输出指令写入 `bufValid[0..3]`。但 InstQueue 的 push FSM 每次只能推 1 条到 FIFO。当 FIFO 满（depth=16）时，push FSM 停滞，新的解码器输出**覆盖**还未推送的 `bufValid` 条目：

```
Cycle N:   响应到达 → 解码输出 inst0,inst1 → bufValid={1,1,0,0}
Cycle N+1: push FSM 推 bufValid[0] → FIFO 入队
Cycle N+2: 新响应到达 → 解码输出 inst2,inst3
           → bufValid(0) := True   ← 覆盖！bufValid[0] 原先的 inst1 丢失！
```

**修复：**

只有 InstQueue 的 staging buffer 为空时才接受新的 carry：

```scala
// InstQueue 新增输出
val bufEmpty = out Bool()  // allEmpty

// Frontend 中门控 carry
}.elsewhen(validReg && queue.io.bufEmpty) {
  carryReg    := decoder.io.carryOut
  hasCarryReg := decoder.io.hasCarryOut
}
```

这样确保上一批指令全部推入 FIFO 后，才接受新的响应数据。

---

## 3. 变更文件

| 文件 | 变更 |
|------|------|
| `Frontend.scala` | `needFetch` 改为 `hasCarryReg \|\| (fetchAddr > lastFetchAddr)`；carry 更新门控 `queue.io.bufEmpty` |
| `InstQueue.scala` | 新增 `io.bufEmpty` 输出（连到 `allEmpty`） |

---

## 4. 验证

| 测试 | 结果 |
|------|------|
| bootrom.bin (141B) | 100 指令无卡死 |
| hello-riscv64-cb-v1.bin (5.5KB) | 325 指令，decode 325/0 失败 |
| cocotb TestHarness | 4/4 PASS |
