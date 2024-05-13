package v1.utils

object ArgParser {

  // TODO check 1 (--,"") check 2 if (--!, ""), failed
  def get(k: String, args : Array[String]) : String = {
    val argsmap = args.grouped(2).map(p => (p(0), p(1))).toList.toMap
    argsmap(k)
  }
}
