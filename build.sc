import mill._, scalalib._

val spinalVersion = "1.8.0"
object ivys {
  val sv = "2.12.11"
  val spinalCore = ivy"com.github.spinalhdl::spinalhdl-core:$spinalVersion"
  val spinalLib = ivy"com.github.spinalhdl::spinalhdl-lib:$spinalVersion"
  val spinalPlugin = ivy"com.github.spinalhdl::spinalhdl-idsl-plugin:$spinalVersion"
  val scalatest = ivy"org.scalatest::scalatest:3.2.2"
  val macroParadise = ivy"org.scalamacros:::paradise:2.1.1"
}
class ChangbaiCommon extends ScalaModule with SbtModule { this: ScalaModule =>
  override def scalaVersion = ivys.sv
  override def scalacPluginIvyDeps = Agg(ivys.macroParadise, ivys.spinalPlugin)
  override def ivyDeps = Agg(ivys.spinalCore, ivys.spinalLib)
  override def scalacOptions = Seq("-Xsource:2.11")
}

object vexriscv extends SbtModule {
  override def millSourcePath = os.pwd / "VexRiscv"
  override def scalaVersion = ivys.sv
}

object changbai extends ChangbaiCommon {
  override def millSourcePath = os.pwd
  def vexLib = vexriscv
  override def moduleDeps: Seq[JavaModule] = Seq(vexLib)
}

