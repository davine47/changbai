module Decode (
        input  clk,
        input  rst,
        input  [63:0] inst,
        output wen,
        output en_imm,
        output [63:0] imm,
        output [3:0]  raddr1,
        output [3:0]  raddr2,
        output [3:0]  waddr
    );

    localparam [6:0] OP_IMM = 7'b0010011;

    localparam [2:0] TYPE_R = 3'b000;
    localparam [2:0] TYPE_I = 3'b001;
    localparam [2:0] TYPE_S = 3'b010;
    localparam [2:0] TYPE_B = 3'b011;
    localparam [2:0] TYPE_U = 3'b100;
    localparam [2:0] TYPE_J = 3'b101;
    localparam [2:0] TYPE_N = 3'b110;

    // inst split signal
    wire [ 6:0] opcode;
    wire [ 4:0] rd;
    wire [ 2:0] funct3;
    wire [ 4:0] rs1;
    wire [ 4:0] rs2;
    wire [ 6:0] funct7;
    wire [63:0] imm_I;
    wire [63:0] imm_S;
    wire [63:0] imm_B;
    wire [63:0] imm_U;
    wire [63:0] imm_J;

    // inst decode signal
    wire [2:0] type;

    // split inst
    assign opcode = inst[ 6: 0];
    assign rd     = inst[11: 7];
    assign funct3 = inst[14:12];
    assign rs1    = inst[19:15];
    assign rs2    = inst[24:20];
    assign funct7 = inst[31:25];
    assign imm_I  = {{20{inst[31]}}, inst[31:20]};
    assign imm_S  = {{20{inst[31]}}, inst[31:25], inst[11:7]};
    assign imm_B  = {{19{inst[31]}}, inst[31], inst[7], inst[30:25], inst[11:8], 1'b0};
    assign imm_U  = {inst[31:12], 12'b0};
    assign imm_J  = {{11{inst[31]}}, inst[19:12], inst[20], inst[30:25], inst[24:21], 1'b0};

    // decode inst
    assign type = (opcode == OP_IMM) ? TYPE_I : TYPE_N;

    // gen ctrl signal
    assign raddr1 = rs1;
    assign raddr2 = rs2;
    assign wen    = (type == TYPE_I) | (type == TYPE_J) | (type == TYPE_U);
    assign waddr  = rd;
    assign en_imm = (type == TYPE_I) | (type == TYPE_S) | (type == TYPE_B) | (type == TYPE_J) | (type == TYPE_U);
    assign imm    = ((type == TYPE_I) & imm_I) 
                  | ((type == TYPE_S) & imm_S) 
                  | ((type == TYPE_B) & imm_B) 
                  | ((type == TYPE_U) & imm_U) 
                  | ((type == TYPE_J) & imm_J);

endmodule //Decode
