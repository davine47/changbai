# 支持文件参考

> 版本: 1.2 | 文件: `v1/Instructions.scala`, `v1/MicroOps.scala`, `v1/utils/Decode.scala`

---

## 1. Instructions.scala

RISC-V 指令编码定义。提供标准指令类型的模板函数和位域常量。

### 内容

- **指令类型模板**: R/I/S/B/U/J 格式的 `Tuple6[T, T, T, T, T, T]` 编码
- **funct3 常量**: `ADD=0, SLL=1, SLT=2, SLTU=3, XOR=4, SRL=5, OR=6, AND=7`
- **原子操作**: ADD/SWAP/AND/OR/XOR/MAX/MIN 等的 funct5 编码
- **CSR 地址**: `CsrRegAddr` object（fflags, frm, fcsr, cycle, time, instret 等）

### 使用方式

```scala
import v1.Instructions._
val template = R(ADD, 10, 0, 20)
```

---

## 2. MicroOps.scala

微操作（Micro-op）编码定义，用于更细粒度的流水线控制。

### 内容

- **MicroOp 枚举类型**: 定义各功能单元的操作码
- **发射队列编码**: 用于指令发射逻辑的位域分配
- **功能单元映射**: 操作→执行单元的路由规则

### 使用方式

```scala
import v1.MicroOps._
```

---

## 3. utils/Decode.scala

抽象译码框架。

### 3.1 AbstractDecodeSigs

```scala
abstract class AbstractDecodeSigs[T <: BaseType](
    needs: DecodeConst,           // 位域常量
    coverAll: Seq[Masked],        // 覆盖所有合法编码的掩码
    spec: DecodingSpec[T]         // 译码映射表
)
```

### 3.2 DecodeConst

位域常量基类：

```scala
abstract class DecodeConst {
  def funct3(from, to): Masked
  def funct7(from, to): Masked
  def opcode(from, to): Masked
  // ...
}
```

### 3.3 Masked

位掩码模板：

```scala
case class Masked(bits: Bits, mask: Bits)
```

### 3.4 DecodingSpec

译码规则表：

```scala
class DecodingSpec[T](val default: T) {
  def addRule(pattern: Masked, value: T): Unit
  def decode(inst: Bits, output: T): Unit
}
```

### 3.5 ScalarDecodeConst/ScalarDecodeSigs

`ScalarDecode` 中使用的具体实例：

```scala
class ScalarDecodeConst extends DecodeConst { ... }
object ScalarDecodeSigs extends AbstractDecodeSigs[Bits] { ... }
```

定义 RV64IMC 全部指令的译码规则。

### 使用方式

```scala
import v1.utils.{AbstractDecodeSigs, DecodeConst}
```

## 4. vector/VectorDecode.scala

RISC-V V 扩展（向量）译码器基础框架。

- `VectorDecodeConst`: V 扩展位域常量
- `VectorDecodeSigs`: V 扩展译码规则表
- 组合逻辑 `switch` 译码 V 指令的 funct6+vm+lmul+sew

---

以上支持文件为各硬件模块提供指令编码常量和译码框架。它们自身不生成 Verilog，但被 `ScalarDecode` 等模块依赖。
