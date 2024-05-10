module adder (
        input  [63:0] src1,
        input  [63:0] src2,
        output [63:0] sum
    );

    assign sum = src1 + src2;

endmodule //adder
