# FetchPredecoder 模块设计文档

> 版本: 1.1 | 文件: `v1/FetchPredecoder.scala`

---

## 1. 概述

取指预译码器。在 64-bit 取指块中识别指令边界，输出每条指令的 size(16/32)+data。功能与 RVCDecoder 类似但输出格式不同——直接输出每条指令的 32-bit 数据（16-bit 压缩指令仅低 16 位有效）。

## 2. 端口

| 信号 | 方向 | 位宽 | 说明 |
|------|------|------|------|
| `io_fetchData` | in | 64 | 取指数据 |
| `io_carryIn` | in | 16 | 上一块残留半字 |
| `io_hasCarryIn` | in | 1 | 有残留 |

| `io_instCount` | out | 3 | 有效指令数 (0-4) |
| `io_inst0Valid` | out | 1 | slot0 有效 |
| `io_inst0Size` | out | 1 | 0=16b, 1=32b |
| `io_inst0Data` | out | 32 | 指令数据 |
| `io_inst1/2/3*` | out | - | 同上 |

| `io_carryOut` | out | 16 | 跨块残留半字 |
| `io_hasCarryOut` | out | 1 | 有跨块残留 |

## 3. 算法

与 RVCDecoder 相同：检查每个半字 inst[1:0] 来确定长度，straddle 时输出 carryOut。

区别：输出 `instData` 包含完整 32-bit（32-bit 指令）或低 16-bit（压缩指令），而非仅 isValid/is32。

## 4. 生成命令

```bash
mill -i changbaiV1.spinal.runMain v1.GenFetchPredecoder
```

输出: `rtl/FetchPredecoder.sv`
