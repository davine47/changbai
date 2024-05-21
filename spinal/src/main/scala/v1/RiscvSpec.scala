package v1

import spinal.core.{Area, B, Bits, Bundle, False, IntToBuilder, LiteralBuilder}

abstract class RiscvSpecs

// TODO: Configuration will inject here later
object RiscvUnPrivSpec extends RiscvSpecs {

  def XLEN = 64

  def IALIGN = 32

  def funct7Range = 31 downto 25

  def rdRange = 11 downto 7

  def funct3Range = 14 downto 12

  def rs1Range = 19 downto 15

  def rs2Range = 24 downto 20

  def rs3Range = 31 downto 27

  def csrRange = 31 downto 20


  case class IMM(instruction: Bits) extends Area {
    // immediates
    def immExtBits = if(RiscvUnPrivSpec.XLEN == 64) 32 else 0

    def i = instruction(31 downto 20)

    def h = instruction(31 downto 24)

    def s = instruction(31 downto 25) ## instruction(11 downto 7)

    def b = instruction(31) ## instruction(7) ## instruction(30 downto 25) ## instruction(11 downto 8)

    def u = instruction(31 downto 12) ## U"x000"

    def j = instruction(31) ## instruction(19 downto 12) ## instruction(20) ## instruction(30 downto 21)

    def z = instruction(19 downto 15)

    // sign-extend immediates
    def i_sext = B((19+immExtBits downto 0) -> i(11)) ## i

    def h_sext = B((23+immExtBits downto 0) -> h(7)) ## h

    def s_sext = B((19+immExtBits downto 0) -> s(11)) ## s

    def b_sext = B((18+immExtBits downto 0) -> b(11)) ## b ## False

    def u_sext = B((0+immExtBits downto 0) -> u.msb) ## u(30 downto 0)

    def j_sext = B((10+immExtBits downto 0) -> j(19)) ## j ## False
  }

}
