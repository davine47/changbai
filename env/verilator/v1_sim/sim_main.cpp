// sim_main.cpp — Verilator testbench for TopV1Sim
//
// Drives clock/reset, DPI-C calls happen automatically inside DUT.
#include <memory>
#include <cstdlib>
#include <cstring>
#include <cstdio>
#include "verilated.h"
#include "verilated_vcd_c.h"
#include "VTopV1Sim.h"

int main(int argc, char** argv) {
    uint64_t max_cycles = 2000;
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "--max-cycles") == 0 && i+1 < argc)
            max_cycles = strtoull(argv[++i], nullptr, 0);
    }

    auto ctx = std::make_unique<VerilatedContext>();
    ctx->traceEverOn(true);
    ctx->commandArgs(argc, argv);

    auto top = std::make_unique<VTopV1Sim>(ctx.get(), "TOP");

    VerilatedVcdC* tfp = new VerilatedVcdC;
    top->trace(tfp, 99);
    tfp->open("dump.vcd");

    // Reset
    top->io_clk = 0;
    top->io_reset = 1;
    for (int i = 0; i < 5; i++) {
        ctx->timeInc(5);
        top->io_clk = 1; top->eval(); tfp->dump(ctx->time());
        ctx->timeInc(5);
        top->io_clk = 0; top->eval(); tfp->dump(ctx->time());
    }
    top->io_reset = 0;

    // Run
    printf("[SIM] Starting simulation (max_cycles=%llu)\n", (unsigned long long)max_cycles);
    for (uint64_t cycle = 0; cycle < max_cycles; cycle++) {
        ctx->timeInc(5);
        top->io_clk = 1; top->eval(); tfp->dump(ctx->time());
        ctx->timeInc(5);
        top->io_clk = 0; top->eval(); tfp->dump(ctx->time());
    }
    printf("[SIM] Done at cycle %llu\n", (unsigned long long)max_cycles);

    tfp->close();
    delete tfp;
    top->final();
    ctx->statsPrintSummary();
    return 0;
}
