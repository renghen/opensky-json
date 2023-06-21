package models.opensky

import javax.inject.Inject
import javax.inject.Named

import akka.actor.ActorRef
import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import play.api.{Configuration, Logger}
import akka.actor.Actor
import akka.actor.Props

class Scheduler @Inject() (
    configuration: Configuration,
    actorSystem: ActorSystem,
    @Named("hello-actor") helloActor: ActorRef
)(implicit
    executionContext: ExecutionContext
) {
  val logger: Logger = Logger(this.getClass())
  val interval = configuration.getMillis("opensky.interval")
  logger.info(s"opensky scheduler interval $interval")

  actorSystem.scheduler.scheduleAtFixedRate(
    initialDelay = 0.microseconds,
    interval = interval.milliseconds,
    receiver = helloActor,
    message = HelloActor.SayHello("tick")
  )
}

object HelloActor {
  def props = Props[HelloActor]()

  case class SayHello(name: String)
}

class HelloActor extends Actor {
  import HelloActor._

  override def receive: Receive = {
    case SayHello(name: String) => {
      println("hello, " + name)
    }
  }
}
