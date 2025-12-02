import mill._
import scalalib._

val spinalVersion = "1.11.0"

val pwd = os.Path(sys.env("MILL_WORKSPACE_ROOT"))

def relatedScalaVersion = "2.13.10"
def chiselScalaVersion = "2.13.16"
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
    "chisel"        -> ivy"org.chipsalliance::chisel:7.0.0-RC1",
    "chisel-plugin" -> ivy"org.chipsalliance:::chisel-plugin:7.0.0-RC1",
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

object vexriscv extends HasSpinalhdl with SbtModule{
  override def millSourcePath = os.pwd / "spinal" / "VexRiscv"
  override def moduleDeps: Seq[JavaModule] = super.moduleDeps
}

class Changbai(chiselGenerator: String = "SimpleGenerator") extends Module{

  object chisel extends Cross[ChiselModules]("chisel")
  trait ChiselModules extends HasChisel with SbtModule {

    def chiselPluginJar: T[Option[PathRef]]
    override def scalacOptions = T(super.scalacOptions() ++ chiselPluginJar().map(path => s"-Xplugin:${path.path}"))

    override def millSourcePath = pwd / "chisel"
    override def mainClass = T(Some(s"generators.${chiselGenerator}"))

    override def moduleDeps: Seq[JavaModule] = super.moduleDeps
  }

  object spinal extends HasSpinalhdl with SbtModule{
    override def millSourcePath = pwd / "spinal"
    def vexriscvModule = vexriscv

    object test extends SbtModuleTests with TestModule.ScalaTest

    override def moduleDeps: Seq[JavaModule] = super.moduleDeps ++ Seq(vexriscvModule)
  }
}

object changbaiV1 extends Changbai

trait Emulator extends Cross.Module2[String, String] {

  val top: String = crossValue
  val chiselGenerator: String = crossValue2

  val cb = new Changbai(chiselGenerator)
  object generator extends Module {

    def elaborate = T {
      os.proc(
        mill.util.Jvm.javaExe,
        "-jar",
        cb.chisel("chisel").assembly().path,
        "--dir", T.dest.toString,
        "--top", top
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
        s"-o=/Users/wenjunnan/projects/yuanqi/targets/changbai/dest"
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

  ("examples.Adder", "SimpleGenerator")
)
