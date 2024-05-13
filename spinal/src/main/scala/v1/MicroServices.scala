package v1
import Instructions._
import spinal.core.internals.Literal
import spinal.core.{B, Bits, Component, False, HardType, IntToBuilder, LiteralBuilder, MaskedLiteral, SpinalEnum, SpinalEnumElement, SpinalEnumEncoding, True, U, default}
import spinal.lib.logic.{DecodingSpec, Masked}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

abstract class MicroService

trait MicroArchDecodeMap {
  def range: Range.Inclusive
  def default: MaskedLiteral
  def table: Array[(MaskedLiteral, MaskedLiteral)]
}

object FnService extends MicroService with MicroArchDecodeMap {

  override def range: Range.Inclusive = (3 downto 0)
  override def default = M"0000"
  override def table: Array[(MaskedLiteral, MaskedLiteral)] = Array(
    ADD -> M"0001",
    SUB -> M"0011"
  )
}

object RegFileService extends MicroService with MicroArchDecodeMap {

  override def range: Range.Inclusive = (5 downto 4)
  override def default = M"00"
  override def table: Array[(MaskedLiteral, MaskedLiteral)] = Array(
    ADD -> M"11"
  )
}

object DecodeService extends MicroService {

  // an implicit service order here
  val serviceList = List[MicroArchDecodeMap](RegFileService, FnService)
  val stringList = ArrayBuffer[String]()
  serviceList.foreach(x => {
    stringList.append(x.default.getBitsString(x.range.size, '-'))
  })
  val mergedDefaultML = MaskedLiteral(stringList.reduce(_ + _))
  val targetWidth = mergedDefaultML.width

  // solve decode op
  val instMapOpcode = mutable.LinkedHashMap[MaskedLiteral, MaskedLiteral]()
  RV32I.foreach(inst => {
    val tmpStringList = ArrayBuffer[String]()
    serviceList.foreach(s => {
      val tmpMap = s.table.toMap
      tmpStringList.append(tmpMap.getOrElse(inst, s.default).getBitsString(s.range.size, '-'))
    })
    instMapOpcode.put(inst, MaskedLiteral(tmpStringList.reduce(_ + _)))
  })
}