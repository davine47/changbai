module Reg #(parameter DATA_WIDTH = 1, parameter RESET_VAL = 0)
    (
        input  clk,
        input  rst,
        input  [DATA_WIDTH - 1 : 0] din,
        output [DATA_WIDTH - 1 : 0] dout
    );

    reg sys_rst;
    reg sys_rst_r;
    reg [DATA_WIDTH - 1:0] dout_r;

    always @(posedge clk or posedge rst)
    begin
        if (rst)
        begin
            sys_rst   <= 1'b1;
            sys_rst_r <= 1'b1;
        end
        else
        begin
            sys_rst_r <= 1'b0;
            sys_rst   <= sys_rst_r;
        end
    end

    always @(posedge clk or posedge sys_rst)
    begin
        if(sys_rst)
        begin
            dout_r <= RESET_VAL;
        end
        else
        begin
            dout_r <= din;
        end
    end

    assign dout = dout_r;

endmodule  //Reg with Synchronized Asynchronous Reset
