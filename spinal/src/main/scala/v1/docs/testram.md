# TestRam 模块设计文档

> 版本: 1.1 | 文件: `v1/testram/TestRam.scala` | 依赖: `Rw64Bus`

---

## 1. 概述

行为级 SRAM 模拟器，用于验证环境。支持 `$readmemb` 从二进制文件初始化（SpinalHDL `mem.initBigInt` 原生支持）。通过 Rw64Bus slave 接口访问。

## 2. 配置

```scala
case class TestRamConfig(
    width: Int,       // 数据位宽（字节）
    depth: Int,       // 深度（条目数，默认 width=1 时一个条目=1 字节）
    initFile: Option[String] = None  // 二进制初始化文件（.bin）
)
```

## 3. 端口

| 信号 | 方向 | 位宽 | 说明 |
|------|------|------|------|
| `io_rw (slave)` | - | Rw64Bus(addrWidth, dataWidth) | RW64 总线从设备接口 |
| `io_clk` | in | 1 | 时钟 |
| `io_reset` | in | 1 | 复位 |

其中 `addrWidth = log2Up(depth * width / 8)`。

## 4. 内部实现

```scala
val mem = Mem(Bits(dataWidth bits), depth)
```

- `dataWidth = width × 8` 位宽访问
- 使用 `mem.initBigInt` 从二进制文件初始化（SpinalHDL 自动生成 `$readmemb` + .bin 文件）

### 读路径

```
rw.raddr  → 字节级地址译码
rw.rvalid → 组合使能 mem.read(upper)
rw.rready → slave 端控制流
rw.rdata  → 拼接 width 个字节
rw.rresp  → rvalid && rready（下一拍脉冲）
```

读取零延迟（组合 readAsync），`rresp` 脉冲在读被接受后下一拍产生。

### 写路径

```
rw.waddr  → 字节级地址译码
rw.wdata  → 拆分 width 个字节
rw.wvalid && rw.wready → mem.write(enable = ...)
```

写入延迟 1 周期（时钟沿更新 mem）。

### 地址截断

外部 64-bit 地址截断到 `addrWidth` 位：

```scala
testRam.io.rw.waddr := frontend.io.rw.waddr(addrWidth-1 downto 0)
```

## 5. 初始化

使用 SpinalHDL 原生 `mem.initBigInt(initData)`，生成 `$readmemb` 语句和对应 `.bin` 文件（Verilator 兼容）。

### 流程

1. 从 elf 生成 bin 文件：
   ```bash
   riscv64-unknown-elf-objcopy -O binary bootrom.elf bootrom.bin
   ```
2. SpinalHDL 的 `loadInitContent()` 读取二进制文件
3. `mem.initBigInt` 生成 Verilog 初始化代码

### Bin 文件格式

每行 64 个二进制字符（对应 64-bit 存储字），共 `depth` 行。

## 6. 生成命令

```bash
mill -i changbaiV1.spinal.runMain v1.testram.GenTestRam
```

输出: `rtl/TestRam.sv`

## 7. 时序

| 路径 | 延迟 |
|------|------|
| 读地址 → 读数据 | 组合（mem.readAsync） |
| rvalid/rready → rresp | 1 周期 |
| 写使能 → mem 更新 | 1 周期（时钟沿） |
