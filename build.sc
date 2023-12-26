import mill._
import scalalib._
import $file.`rocket-chip`.common
import $file.`rocket-chip`.cde.common
import $file.`rocket-chip`.hardfloat.build

val spinalVersion = "1.9.3"

def relatedScalaVersion = "2.13.10"
def chiselScalaVersion = "2.13.10"
def spinalhdlScalaVersion = "2.11.12"

val defaultSpinalhdlVersions = Map(
    "spinalCore"    -> ivy"com.github.spinalhdl::spinalhdl-core:$spinalVersion",
    "spinalLib"     -> ivy"com.github.spinalhdl::spinalhdl-lib:$spinalVersion",
    "spinalPlugin"  -> ivy"com.github.spinalhdl::spinalhdl-idsl-plugin:$spinalVersion",
    "scalatest"     -> ivy"org.scalatest::scalatest:3.2.2",
    "macroParadise" -> ivy"org.scalamacros:::paradise:2.1.1",
    "yaml"          -> ivy"org.yaml:snakeyaml:1.8"
)

def defaultChiselVersions(chiselVersion: String) = chiselVersion match {
  case "chisel" => Map(
    "chisel"        -> ivy"org.chipsalliance::chisel:6.0.0-RC1",
    "chisel-plugin" -> ivy"org.chipsalliance:::chisel-plugin:6.0.0-RC1",
    "chiseltest"    -> ivy"edu.berkeley.cs::chiseltest:5.0.2"
  )
  case "chisel3" => Map(
    "chisel"        -> ivy"edu.berkeley.cs::chisel3:3.6.0",
    "chisel-plugin" -> ivy"edu.berkeley.cs:::chisel3-plugin:3.6.0",
    "chiseltest"    -> ivy"edu.berkeley.cs::chiseltest:0.6.2"
  )
}

trait HasSpinalhdl extends ScalaModule  {

  val spinalhdlDeps = Agg(defaultSpinalhdlVersions("spinalCore"), 
                          defaultSpinalhdlVersions("spinalLib"), 
                          defaultSpinalhdlVersions("yaml"))

  val spinalhdlPluginDeps = Agg(defaultSpinalhdlVersions("macroParadise"), 
                                defaultSpinalhdlVersions("spinalPlugin"))

  override def scalaVersion = spinalhdlScalaVersion
  override def scalacPluginIvyDeps = spinalhdlPluginDeps
  override def ivyDeps = spinalhdlDeps
  override def scalacOptions = Seq("-Xsource:2.11")
}

trait HasChisel extends SbtModule with Cross.Module[String] {

  def chiselModule: Option[ScalaModule] = None

  def chiselPluginJar: T[Option[PathRef]] = None

  def defaultScalaVersion: String = chiselScalaVersion

  def chiselIvy: Option[Dep] = Some(defaultChiselVersions(crossValue)("chisel"))

  def chiselPluginIvy: Option[Dep] = Some(defaultChiselVersions(crossValue)("chisel-plugin"))

  override def scalaVersion = defaultScalaVersion

  override def scalacOptions = super.scalacOptions() ++ 
    Agg("-language:reflectiveCalls", "-Ymacro-annotations", "-Ytasty-reader")

  val chiselDeps = Agg(chiselIvy.get)
  val chiselPluginDeps =  Agg(chiselPluginIvy.get)

  override def ivyDeps = super.ivyDeps() ++ chiselDeps
  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ chiselPluginDeps
}

object rocketchip extends Cross[RocketChip]("chisel", "chisel3")

trait RocketChip
  extends millbuild.`rocket-chip`.common.RocketChipModule
    with HasChisel {
  def scalaVersion: T[String] = T(defaultScalaVersion)

  override def millSourcePath = os.pwd / "rocket-chip"

  def macrosModule = macros

  def hardfloatModule = hardfloat(crossValue)

  def cdeModule = cde

  def mainargsIvy = ivy"com.lihaoyi::mainargs:0.5.4"

  def json4sJacksonIvy = ivy"org.json4s::json4s-jackson:4.0.6"

  object macros extends Macros

  trait Macros
    extends millbuild.`rocket-chip`.common.MacrosModule
      with SbtModule {

    def scalaVersion: T[String] = T(defaultScalaVersion)

    def scalaReflectIvy = ivy"org.scala-lang:scala-reflect:${defaultScalaVersion}"
  }

  object hardfloat extends Cross[Hardfloat](crossValue)

  trait Hardfloat
    extends millbuild.`rocket-chip`.hardfloat.common.HardfloatModule with HasChisel {

    def scalaVersion: T[String] = T(defaultScalaVersion)

    override def millSourcePath = os.pwd / "rocket-chip" / "hardfloat" / "hardfloat"

  }

  object cde extends CDE

  trait CDE extends millbuild.`rocket-chip`.cde.common.CDEModule with ScalaModule {

    def scalaVersion: T[String] = T(defaultScalaVersion)

    override def millSourcePath = os.pwd / "rocket-chip" / "cde" / "cde"
  }
}

object vexriscv extends HasSpinalhdl with SbtModule{
  override def millSourcePath = os.pwd / "VexRiscv"
  override def moduleDeps: Seq[JavaModule] = super.moduleDeps
}

object changbai extends Module{

  override def millSourcePath = os.pwd
  object chisel extends Cross[ChiselModules]("chisel", "chisel3")
  trait ChiselModules extends HasChisel {

    def rocketModule = rocketchip(crossValue)
    override def moduleDeps: Seq[JavaModule] = super.moduleDeps ++ Seq(rocketModule)
  }
  object spinal extends HasSpinalhdl {
    
    def vexriscvModule = vexriscv
    override def moduleDeps: Seq[JavaModule] = super.moduleDeps ++ Seq(vexriscvModule)
  }
}





