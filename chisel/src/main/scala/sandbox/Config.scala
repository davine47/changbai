package sandbox

import org.chipsalliance.cde.config.{Config, Field}

case object sdXLen extends Field[Int]
class HelloConfig extends Config((site, here, up) => {
  case sdXLen => 3
})
