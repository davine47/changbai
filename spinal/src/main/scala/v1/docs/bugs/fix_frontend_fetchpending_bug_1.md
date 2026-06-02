# fix_frontend_fetchpending_bug_1: FSM carry 链重复取指导致 lastFetchAddr 逃逸

> 日期: 2026-05-29 | 影响模块: `v1/Frontend.scala`

---

## 1. 问题现象

hello 程序中的 `fmv.w.x` 连续 32-bit 指令序列完全丢失，输出流中看不到任何 `0xf0000xxx` 编码的指令。

```
修复前:
  bootrom: 100 指令 ✓
  hello:    35 指令 ✗  (fmv 全部丢失, push=35 pop=35)

修复后:
  bootrom: 100 指令 ✓
  hello:   500 指令 ✓  (fmv 序列完整出现, push=501 pop=499)
```

## 2. 根因分析

### 2.1 `hasCarryReg=1` 期间的 FSM 行为

fmv.w.x 序列每 64-bit chunk 都在边界处 straddle，carry 链极长：

```
chunk 0x80: csrwi + fmv ft0(straddle)  → hasCarryReg=1
chunk 0x88: ft0(carry) + ft1 + ft2(straddle) → hasCarryReg=1
chunk 0x90: ft2(carry) + ft3 + ft4(straddle) → hasCarryReg=1
... ~24 chunks of continuous carry ...
```

**旧 FSM 的致命循环：**

```scala
// 旧代码: needFetch 在 hasCarryReg=1 期间永远为真
val needFetch = hasCarryReg || (fetchAddr =/= lastFetchAddr)

// FSM 在每个时钟沿反复进入取指:
// 拍 N:   fetchReq=0, needFetch=1 → fetchReq=1
// 拍 N+1: fetchReq=1, reqReady=1 → fetchReq=0
// 拍 N+2: fetchReq=0, needFetch=1 → fetchReq=1  ← 又取！
```

`hasCarryReg` 只有在 `validReg=1`(新响应到达)时才更新，而响应需要 ~4 拍。在这 4 拍内，FSM 每拍发起一次取指，`lastFetchAddr` 每拍 +8，迅速逃逸到远超 `nextPcReg` 的位置。

### 2.2 数据丢失链路

```
FSM 逃逸期间:
  lastFetchAddr: 0x80 → 0x88 → 0x90 → ... → 0x140
  发出的取指: 0x88, 0x90, 0x98, ..., 0x140 (共 ~24 个)
  
TestRam 逐个响应:
  响应 0x88 到达 → 解码 fmv ft1,ft2 → bufValid 捕获
  响应 0x90 到达 → 解码 fmv ft3,ft4 → bufValid 覆盖 (旧数据丢失!)
  ...
```

当后续响应追上来时，`lastFetchAddr` 已远超前，而 `nextPcReg` 只推进到 csrwi 之后（0x8A 左右）。`fetchAddr(0x88) < lastFetchAddr(0x140)`，FSM 停止取指。但丢失的 fmv 数据已无法恢复。

## 3. 修复

### 3.1 新增 `fetchPending` 标志

```scala
val fetchPending = Reg(Bool()) init False

// 取指被接受时置位
}.elsewhen(io.toFetch.reqReady) {
  fetchReq     := False
  fetchPending := True   // 等待响应，禁止再次取指
}

// 响应到达时清除 (放在 validReg 定义之后)
when(validReg) {
  fetchPending := False
}

// needFetch 门控
val needFetch = (hasCarryReg || (fetchAddr > lastFetchAddr)) && !fetchPending
```

`fetchPending` 确保一次只有一笔取指在途，carry 链中 FSM 不会再狂发取指。

### 3.2 carry 时取下一连续地址

```scala
lastFetchAddr := hasCarryReg ? (lastFetchAddr + 8) | fetchAddr
```

有剩余半字时取 `lastFetchAddr+8`（下一连续 chunk），而非当前对齐地址。

### 3.3 `>` 替代 `!=`

```scala
// 旧: fetchAddr != lastFetchAddr  → 超前取指后仍然触发回取旧地址
// 新: fetchAddr > lastFetchAddr   → 只有 nextPc 真正推进后才取指
val needFetch = (hasCarryReg || (fetchAddr > lastFetchAddr)) && !fetchPending
```

无 carry 时，只有 `nextPc` 超过上次取指位置才发起新取指，避免回取已取过的块。

## 4. 变更文件

| 文件 | 变更 |
|------|------|
| `Frontend.scala` | 新增 `fetchPending` 寄存器；`needFetch` 改为 `>` + `!fetchPending` 门控；`lastFetchAddr` carry 时 +8 |

## 5. 验证

| 测试 | 结果 |
|------|------|
| bootrom.bin (141B) | 100 指令，decode 100/0 ✓ |
| hello-riscv64-cb-v1.bin (5.5KB) | 500 指令，fmv 序列完整，decode 500/0 ✓ |
| cocotb TestHarness | 4/4 PASS |
