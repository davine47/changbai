// This file is AI[DeepSeek V4 Pro, high]-generated and manually verified.
// DPI-C wrapper for difftest_decode
// Port names use io_ prefix (SpinalHDL default)
module IseeDecode (
  input  wire        io_clock,
  input  wire        io_valid,
  input  wire [63:0] io_pc,
  input  wire [31:0] io_instruction,
  input  wire        io_isRVC,
  input  wire        io_ill,
  input  wire        io_legal,
  input  wire [4:0]  io_aluOp,
  input  wire        io_branch,
  input  wire        io_jal,
  input  wire        io_jalr,
  input  wire        io_useMem,
  input  wire        io_useCsr
);

  import "DPI-C" function void isee_decode(
    input longint pc,
    input int     instruction,
    input byte    isRVC,
    input byte    ill,
    input byte    legal,
    input byte    aluOp,
    input byte    branch,
    input byte    jal,
    input byte    jalr,
    input byte    useMem,
    input byte    useCsr
  );

  always @(posedge io_clock) begin
    if (io_valid) begin
      isee_decode(io_pc, io_instruction,
        {7'd0, io_isRVC}, {7'd0, io_ill}, {7'd0, io_legal},
        {3'd0, io_aluOp},
        {7'd0, io_branch}, {7'd0, io_jal}, {7'd0, io_jalr},
        {7'd0, io_useMem}, {7'd0, io_useCsr});
    end
  end

endmodule
