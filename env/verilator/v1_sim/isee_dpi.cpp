// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
// isee_dpi.cpp — C backend for IseeDecode DPI-C
//
// Receives decode events from RTL via SystemVerilog DPI-C.
// Calls spike-dasm for disassembly on each instruction.

#include <cstdio>
#include <cstdint>
#include <cstdlib>
#include <cstring>

// ---- spike-dasm disassembly ----
static const char* dasm(uint32_t inst) {
    static char buf[128];
    static char cmd[256];
    snprintf(cmd, sizeof(cmd),
             "echo 'DASM(%08X)' | ~/riscv-isa-sim/build/spike-dasm 2>/dev/null", inst);
    FILE* fp = popen(cmd, "r");
    if (!fp) return "?";
    if (fgets(buf, sizeof(buf), fp)) {
        size_t len = strlen(buf);
        if (len > 0 && buf[len-1] == '\n') buf[len-1] = '\0';
    } else {
        buf[0] = '\0';
    }
    pclose(fp);
    return buf[0] ? buf : "?";
}

// ---- ALU opcode names ----
static const char* aluOpName(uint8_t op) {
    static const char* names[32] = {
        [0x00]="ADD",  [0x01]="SLL",  [0x02]="SLT",  [0x03]="SLTU",
        [0x04]="XOR",  [0x05]="SRL",  [0x06]="OR",   [0x07]="AND",
        [0x08]="ADDW", [0x09]="SLLW",
        [0x0a]="LUI",  [0x0b]="AUIPC",
        [0x10]="SUB",  [0x18]="SUBW",
        [0x0d]="SRLW", [0x1d]="SRAW",
        [0x15]="SRA",
    };
    const char* n = names[op & 0x1f];
    return n ? n : "?";
}

// ---- Memory operation names ----
static const char* memOpName(uint8_t op) {
    static const char* names[32] = {
        [0x00]="LB", [0x01]="LH", [0x02]="LW", [0x03]="LD",
        [0x04]="SB", [0x05]="SH", [0x06]="SW", [0x07]="SD",
    };
    const char* n = names[op & 0x1f];
    return n ? n : "?";
}

// ---- CSR operation names ----
static const char* csrOpName(uint8_t op) {
    static const char* names[32] = {
        [0x01]="RW",  [0x02]="RS",  [0x03]="RC",
        [0x04]="RWI", [0x05]="RSI", [0x06]="RCI",
    };
    const char* n = names[op & 0x1f];
    return n ? n : "0";
}

extern "C" void isee_decode(
    uint64_t pc,
    uint32_t instruction,
    uint8_t  isRVC,
    uint8_t  ill,
    uint8_t  legal,
    uint8_t  aluOp,
    uint8_t  branch,
    uint8_t  jal,
    uint8_t  jalr,
    uint8_t  useMem,
    uint8_t  memOp,
    uint8_t  useCsr,
    uint8_t  csrOp)
{
    static uint64_t count = 0;
    count++;

    const char* rvc   = isRVC ? "RVC" : "32b";
    const char* legal_str = (legal && !ill) ? "LEGAL" : "EXCEPTION_2";

    // Build opcode field from relevant sub-decodes
    char opstr[32] = "";
    if (useMem) {
        snprintf(opstr, sizeof(opstr), "%s", memOpName(memOp));
    } else if (useCsr) {
        snprintf(opstr, sizeof(opstr), "CSR_%s", csrOpName(csrOp));
    } else {
        snprintf(opstr, sizeof(opstr), "%s", aluOpName(aluOp));
    }

    printf("[DIF] %4llu | 0x%08x | 0x%08x | %s %-11s | %-7s | %-30s",
           (unsigned long long)count,
           (unsigned int)pc, instruction,
           rvc, legal_str, opstr,
           dasm(instruction));

    // Flags column: ALU op name only for ALU operations;
    // MEM/CSR/BR/JAL/JALR are always in this column
    if (useMem)      printf(" MEM");
    else if (useCsr) printf(" CSR");
    else             printf(" %s", aluOpName(aluOp));
    if (branch) printf(" BR");
    if (jal)    printf(" JAL");
    if (jalr)   printf(" JALR");
    printf("\n");
    fflush(stdout);
}
