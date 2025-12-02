// Generator : SpinalHDL v1.13.0    git head : d9d72474863badf47d8585d187f3e04ae4749c59
// Component : MyPlay
// Git hash  : 784c772a23e63661c98a94bb9717bc3efe2863ff

`timescale 1ns/1ps 
module MyPlay (
  input  wire [7:0]    a_0,
  input  wire [7:0]    a_1,
  input  wire [7:0]    a_2,
  input  wire [7:0]    a_3,
  input  wire [7:0]    a_4,
  input  wire [7:0]    a_5,
  input  wire [7:0]    a_6,
  input  wire [7:0]    a_7,
  input  wire [7:0]    mask,
  output wire [7:0]    b
);

  wire                _zz_b;
  wire                _zz_b_1;
  wire                _zz_b_2;
  wire                _zz_b_3;

  assign _zz_b = mask[0]; // @ BaseType.scala l308
  assign _zz_b_1 = mask[2]; // @ BaseType.scala l308
  assign _zz_b_2 = mask[4]; // @ BaseType.scala l308
  assign _zz_b_3 = (_zz_b || mask[1]); // @ BaseType.scala l308
  assign b = ((_zz_b_3 || (_zz_b_1 || mask[3])) ? (_zz_b_3 ? (_zz_b ? a_0 : a_1) : (_zz_b_1 ? a_2 : a_3)) : ((_zz_b_2 || mask[5]) ? (_zz_b_2 ? a_4 : a_5) : (mask[6] ? a_6 : a_7))); // @ Play.scala l10

endmodule
