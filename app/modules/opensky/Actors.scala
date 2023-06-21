package modules.opensky

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

import models.opensky.HelloActor

class Actors extends AbstractModule with AkkaGuiceSupport {
  override def configure = {
    bindActor[HelloActor]("hello-actor")
  }
}
