package v1
import Instructions._
import spinal.core.{IntToBuilder, LiteralBuilder, MaskedLiteral}
import spinal.lib._
import spinal.core._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

abstract class MicroService

trait MicroArchDecodeMap {
  def range: Range.Inclusive
  def default: MaskedLiteral
  def table: Array[(MaskedLiteral, MaskedLiteral)]
}

object SimpleRobService extends  MicroService {
  val ROB_ENTRY_NUM = 64
  val ROB_IDX_WIDTH = log2Up(ROB_ENTRY_NUM)
}

object SimpleRsService extends MicroService {
  def TAG_WIDTH = 4
  def RS_ENTRY_NUM = 4
  def FULL_ALU_NUM = 2
  def LITE_ALU_NUM = 1
  def SUM_ALU_NUM = 1
}

object FnService extends MicroService with MicroArchDecodeMap {

  def FN_ADD_SUB = "0000"
  def FN_SLT_SLTU = "0100"
  def FN_OR = "1000"
  def FN_XOR = "1001"
  def FN_AND = "1010"
  def FN_SLL = "1100"
  def FN_SRL = "1101"
  def FN_SRA = "1110"

  override def range: Range.Inclusive = (3 downto 0)
  override def default = M"0000"
  override def table: Array[(MaskedLiteral, MaskedLiteral)] = Array(
    ADD   -> MaskedLiteral(FN_ADD_SUB),
    SUB   -> MaskedLiteral(FN_ADD_SUB),
    SLT   -> MaskedLiteral(FN_SLT_SLTU),
    SLTU  -> MaskedLiteral(FN_SLT_SLTU),
    XOR   -> MaskedLiteral(FN_XOR),
    OR    -> MaskedLiteral(FN_OR),
    AND   -> MaskedLiteral(FN_AND),
    ADDI  -> MaskedLiteral(FN_ADD_SUB),
    SLTI  -> MaskedLiteral(FN_SLT_SLTU),
    SLTIU -> MaskedLiteral(FN_SLT_SLTU),
    XORI  -> MaskedLiteral(FN_XOR),
    ORI   -> MaskedLiteral(FN_OR),
    ANDI  -> MaskedLiteral(FN_AND),
    LUI   -> MaskedLiteral(FN_ADD_SUB),
    AUIPC -> MaskedLiteral(FN_ADD_SUB),
    SLL   -> MaskedLiteral(FN_SLL),
    SRL   -> MaskedLiteral(FN_SRL),
    SRA   -> MaskedLiteral(FN_SRA),
    SLLI  -> MaskedLiteral(FN_SLL),
    SRLI  -> MaskedLiteral(FN_SRL),
    SRAI  -> MaskedLiteral(FN_SRA)
  )
  def ALU_FUNC_RANGE1 = (3 downto 2)
  def ALU_FUNC_RANGE2 = (1 downto 0)

}

object SrcLessUnsignedService extends MicroService with MicroArchDecodeMap {

  def isUnsigned = "1"
  def isSigned = "0"
  override def range: Range.Inclusive = (4 downto 4)
  override def default: MaskedLiteral = M"0"
  override def table: Array[(MaskedLiteral, MaskedLiteral)] = Array(
    SLT   -> MaskedLiteral(isSigned),
    SLTU  -> MaskedLiteral(isUnsigned),
    SLTI  -> MaskedLiteral(isSigned),
    SLTIU -> MaskedLiteral(isUnsigned),
    BLT   -> MaskedLiteral(isSigned),
    BLTU  -> MaskedLiteral(isUnsigned),
    BGE   -> MaskedLiteral(isSigned),
    BGEU  -> MaskedLiteral(isUnsigned)
  )

}

object SrcUseSubLess extends MicroService with MicroArchDecodeMap {
  def notUseSubLess = "0"
  def useSubLess = "1"
  override def range: Range.Inclusive = (5 downto 5)
  override def default: MaskedLiteral = M"0"
  override def table: Array[(MaskedLiteral, MaskedLiteral)] = Array(
    ADD   -> MaskedLiteral(notUseSubLess),
    SUB   -> MaskedLiteral(useSubLess),
    SLT   -> MaskedLiteral(useSubLess),
    SLTU  -> MaskedLiteral(useSubLess),
    ADDI  -> MaskedLiteral(notUseSubLess),
    SLTI  -> MaskedLiteral(useSubLess),
    SLTIU -> MaskedLiteral(useSubLess),
    LUI   -> MaskedLiteral(notUseSubLess),
    AUIPC -> MaskedLiteral(notUseSubLess)
  )
}

object RegFileService extends MicroService with MicroArchDecodeMap {

  override def range: Range.Inclusive = (7 downto 6)
  override def default = M"00"
  override def table: Array[(MaskedLiteral, MaskedLiteral)] = Array(
    ADD -> M"11",
    ADDI -> M"10"
  )
}

object DecodeService extends MicroService {

  // an implicit service order here
  val serviceList = List[MicroArchDecodeMap](RegFileService, SrcUseSubLess, SrcLessUnsignedService, FnService)
  val stringList = ArrayBuffer[String]()
  serviceList.foreach(x => {
    stringList.append(x.default.getBitsString(x.range.size, '-'))
  })
  val mergedDefaultML = MaskedLiteral(stringList.reduce(_ + _))
  val targetWidth = mergedDefaultML.width
  // solve decode op
  val instMapOpcode = mutable.LinkedHashMap[MaskedLiteral, MaskedLiteral]()
  RV64I.foreach(inst => {
    val tmpStringList = ArrayBuffer[String]()
    serviceList.foreach(s => {
      val tmpMap = s.table.toMap
      val p = tmpMap.getOrElse(inst, s.default).getBitsString(s.range.size, '-')
      tmpStringList.append(p)
    })
    instMapOpcode.put(inst, MaskedLiteral(tmpStringList.reduce(_ + _)))
  })
}