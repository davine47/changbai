import mill._
import scalalib._
import $file.chisel.`rocket-chip`.common
import $file.chisel.`rocket-chip`.cde.common
import $file.chisel.`rocket-chip`.hardfloat.common

val spinalVersion = "1.11.0"

val pwd = os.Path(sys.env("MILL_WORKSPACE_ROOT"))

def relatedScalaVersion = "2.13.10"
def chiselScalaVersion = "2.13.15"
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
    "chisel"        -> ivy"org.chipsalliance::chisel:6.6.0",
    "chisel-plugin" -> ivy"org.chipsalliance:::chisel-plugin:6.6.0",
    "chiseltest"    -> ivy"edu.berkeley.cs::chiseltest:6.0.0",
    "mainargs"      -> ivy"com.lihaoyi::mainargs:0.5.0"
  )
  case "chisel3" => Map(
    "chisel"        -> ivy"edu.berkeley.cs::chisel3:3.6.0",
    "chisel-plugin" -> ivy"edu.berkeley.cs:::chisel3-plugin:3.6.0",
    "chiseltest"    -> ivy"edu.berkeley.cs::chiseltest:0.6.2",
    "mainargs"      -> ivy"com.lihaoyi::mainargs:0.5.0"
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

  def changbaiMainargsIvy : Option[Dep] = Some(defaultChiselVersions(crossValue)("mainargs"))

  override def scalaVersion = defaultScalaVersion

  override def scalacOptions = super.scalacOptions() ++ 
    Agg("-language:reflectiveCalls", "-Ymacro-annotations", "-Ytasty-reader")

  val compileDeps = Agg(changbaiMainargsIvy.get)
  val chiselDeps = Agg(chiselIvy.get)
  val chiselPluginDeps =  Agg(chiselPluginIvy.get)

  override def ivyDeps = super.ivyDeps() ++ chiselDeps ++ compileDeps
  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ chiselPluginDeps
}

object rocketchip extends Cross[RocketChip]("chisel", "chisel3")

trait RocketChip
  extends $file.`chisel`.`rocket-chip`.common.RocketChipModule
    with HasChisel {
  override def scalaVersion: T[String] = T(defaultScalaVersion)

  override def millSourcePath = pwd / "chisel" /"rocket-chip"

  def macrosModule = macros

  def hardfloatModule = hardfloat(crossValue)

  def cdeModule = cde

  def mainargsIvy = ivy"com.lihaoyi::mainargs:0.5.4"

  def json4sJacksonIvy = ivy"org.json4s::json4s-jackson:4.0.6"

  object macros extends Macros

  trait Macros
    extends $file.`chisel`.`rocket-chip`.common.MacrosModule
      with SbtModule {

    def scalaVersion: T[String] = T(defaultScalaVersion)

    def scalaReflectIvy = ivy"org.scala-lang:scala-reflect:${defaultScalaVersion}"
  }

  object hardfloat extends Cross[Hardfloat](crossValue)

  trait Hardfloat
    extends $file.`chisel`.`rocket-chip`.hardfloat.common.HardfloatModule with HasChisel {

    override def scalaVersion: T[String] = T(defaultScalaVersion)

    override def millSourcePath = pwd / "chisel" /"rocket-chip" / "hardfloat" / "hardfloat"

  }

  object cde extends CDE

  trait CDE extends $file.`chisel`.`rocket-chip`.cde.common.CDEModule with ScalaModule {

    def scalaVersion: T[String] = T(defaultScalaVersion)

    override def millSourcePath = pwd / "chisel" / "rocket-chip" / "cde" / "cde"
  }
}

object vexriscv extends HasSpinalhdl with SbtModule{
  override def millSourcePath = os.pwd / "spinal" / "VexRiscv"
  override def moduleDeps: Seq[JavaModule] = super.moduleDeps
}

class Changbai(chiselGenerator: String = "SimpleGenerator") extends Module{

  object chisel extends Cross[ChiselModules]("chisel", "chisel3")
  trait ChiselModules extends HasChisel with SbtModule {

    def chiselPluginJar: T[Option[PathRef]]
    override def scalacOptions = T(super.scalacOptions() ++ chiselPluginJar().map(path => s"-Xplugin:${path.path}"))

    override def millSourcePath = pwd / "chisel"
    override def mainClass = T(Some(s"generators.${chiselGenerator}"))

    def rocketModule = rocketchip(crossValue)
    override def moduleDeps: Seq[JavaModule] = super.moduleDeps ++ Seq(rocketModule)
  }

  object spinal extends HasSpinalhdl with SbtModule{
    override def millSourcePath = pwd / "spinal"
    def vexriscvModule = vexriscv

    object test extends SbtModuleTests with TestModule.ScalaTest

    override def moduleDeps: Seq[JavaModule] = super.moduleDeps ++ Seq(vexriscvModule)
  }
}

object changbaiV1 extends Changbai

trait Emulator extends Cross.Module3[String, String, String] {
  
  val top: String = crossValue
  val config: String = crossValue2
  val chiselGenerator: String = crossValue3

  val cb = new Changbai(chiselGenerator)
  object generator extends Module {

    def elaborate = T {
      os.proc(
        mill.util.Jvm.javaExe,
        "-jar",
        cb.chisel("chisel").assembly().path,
        "--dir", T.dest.toString,
        "--top", top,
        "--config", config,
      ).call()
      PathRef(T.dest)
    }
  
    def chiselAnno = T {
      os.walk(elaborate().path).collectFirst { case p if p.last.endsWith("anno.json") => p }.map(PathRef(_)).get
    }

    def chirrtl = T {
      os.walk(elaborate().path).collectFirst { case p if p.last.endsWith("fir") => p }.map(PathRef(_)).get
    }
  }

  object mfccompiler extends Module {
    def compile = T {
      println(generator.chirrtl().path)
      println(generator.chiselAnno().path)
      println(T.dest)
      os.proc("firtool",
        generator.chirrtl().path,
        s"--annotation-file=${generator.chiselAnno().path}",
        "--disable-annotation-unknown",
        "--disable-all-randomization",
        "-O=debug",
        "--split-verilog",
        "--preserve-values=named",
        "--output-annotation-file=mfc.anno.json",
        s"-o=${T.dest}"
      ).call(T.dest)
      PathRef(T.dest)
    }
    // here is build chain
    def rtls = T {
      os.read(compile().path / "filelist.f").split("\n").map(str =>
        try {
          os.Path(str)
        } catch {
          case e: IllegalArgumentException if e.getMessage.contains("is not an absolute path") =>
            compile().path / str.stripPrefix("./")
        }
      ).filter(p => p.ext == "v" || p.ext == "sv").map(PathRef(_)).toSeq
    }
  }

}

object emulator extends Cross[Emulator](

  ("sandbox.Hello", "sandbox.HelloConfig", "DiplomacyGenerator"),
  ("examples.Adder", "sandbox.HelloConfig", "SimpleGenerator")
)
