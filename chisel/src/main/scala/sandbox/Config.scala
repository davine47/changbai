package sandbox

import org.chipsalliance.cde.config.{Config, Field}

case object XLen extends Field[Int]
class HelloConfig extends Config((site, here, up) => {
  case XLen => 3
})
