# SignExt 模块设计文档

> 版本: 1.1 | 文件: `v1/SignExt.scala` | 依赖: VexRiscv IMM 逻辑

---

## 1. 概述

RISC-V 指令立即数字段提取与符号扩展模块。从 32-bit 指令中按 `immExtType` 选择立即数类型，提取对应的位域，并符号扩展（或零扩展）至 xlen（32 或 64 位）。

立即数字段提取逻辑源自 VexRiscv `Riscv.scala` 中的 `IMM`。

## 2. 端口

| 信号 | 方向 | 位宽 | 说明 |
|------|------|------|------|
| `io_instruction` | in | 32 | 32-bit RISC-V 指令 |
| `io_immExtType` | in | 3 | 立即数类型编码 |
| `io_immediate` | out | xlen | 符号/零扩展后的立即数 |

## 3. 立即数类型编码

| 编码 | 类型 | 说明 | 示例指令 |
|------|------|------|----------|
| 001 | S | S-type store offset（12-bit 有符号） | SB, SH, SW, SD |
| 010 | SB | B-type branch offset（13-bit 有符号，bit0=0） | BEQ, BNE, BLT, BGE |
| 011 | U | U-type upper immediate（32-bit 零扩展） | LUI, AUIPC |
| 100 | UJ | J-type jump offset（21-bit 有符号，bit0=0） | JAL |
| 101 | I | I-type immediate（12-bit 有符号） | ADDI, ORI, Load, CSR* |
| 110 | Z | Z-type（5-bit 零扩展，inst[19:15]=rs1） | rs1 域提取 |
| 000 | - | 保留（输出 0） | - |

## 4. 立即数字段提取（VexRiscv IMM 逻辑）

```scala
// I-type: inst[31:20]
val i_imm = instruction(31 downto 20)

// S-type: inst[31:25] ## inst[11:7]
val s_imm = instruction(31 downto 25) ## instruction(11 downto 7)

// B-type: inst[31] ## inst[7] ## inst[30:25] ## inst[11:8]
val b_imm = instruction(31) ## instruction(7) ##
            instruction(30 downto 25) ## instruction(11 downto 8)

// U-type: inst[31:12] << 12
val u_imm = instruction(31 downto 12) ## U"x000"

// J-type: inst[31] ## inst[19:12] ## inst[20] ## inst[30:21]
val j_imm = instruction(31) ## instruction(19 downto 12) ##
            instruction(20) ## instruction(30 downto 21)

// Z-type: inst[19:15] (rs1 field)
val z_imm = instruction(19 downto 15)
```

## 5. 符号扩展

| 类型 | 位宽 | 扩展方式 | 代码 |
|------|------|----------|------|
| I | 12-bit | sext → xlen | `B((xlen-13 downto 0) → imm(11)) ## imm` |
| S | 12-bit | sext → xlen | 同上 |
| SB | 13-bit | sext → xlen | `B((xlen-14 downto 0) → imm(11)) ## imm ## False` |
| U | 32-bit | zero-ext → xlen | `B(0, xlen-32) ## imm` |
| UJ | 21-bit | sext → xlen | `B((xlen-22 downto 0) → imm(19)) ## imm ## False` |
| Z | 5-bit | zero-ext → xlen | `B(0, xlen-5) ## imm` |

## 6. 输出选择

```scala
switch(io.immExtType) {
    is(1) { result := s_sext  }  // S-type
    is(2) { result := b_sext  }  // B-type
    is(3) { result := u_ext   }  // U-type
    is(4) { result := j_sext  }  // J-type
    is(5) { result := i_sext  }  // I-type
    is(6) { result := z_ext   }  // Z-type
    default { result := 0     }  // 类型 0, 7 → 0
}
```

## 7. 顶层封装

`SignExtTop` — 带时钟的顶层用于验证：

| 信号 | 方向 | 说明 |
|------|------|------|
| `io_clk` | in | 时钟 |
| `io_reset` | in | 复位 |
| `io_instruction` | in | 32-bit 指令 |
| `io_immExtType` | in | 立即数类型 |
| `io_immediate` | out | 扩展结果 |

## 8. 生成命令

```bash
make signext
```

输出: `rtl/SignExt.sv`, `rtl/SignExtTop.sv`

## 9. 验证

```bash
cd env/coco_tb/SignExt && make
```

- 9 个测试，9/9 PASS
- 随机测试 2500 cycles
- 综合测试 2000+ cycles
- VCD 波形：`dump.vcd`
