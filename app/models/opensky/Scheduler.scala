// package models.opensky

// import javax.inject.Inject
// import javax.inject.Named

// import akka.actor.ActorRef
// import akka.actor.ActorSystem

// import scala.concurrent.ExecutionContext
// import scala.concurrent.duration._

// import play.api.{Configuration, Logger}

// class Scheduler @Inject() (
//     configuration: Configuration,
//     actorSystem: ActorSystem,
//     @Named("fetchTimeAndState-actor") fetchTimeAndStateActor: ActorRef
// )(implicit
//     executionContext: ExecutionContext
// ) {
//   val logger: Logger = Logger(this.getClass())
//   val interval = configuration.getMillis("opensky.interval")
//   logger.info(s"opensky scheduler interval $interval")

//   actorSystem.scheduler.scheduleAtFixedRate(
//     initialDelay = 0.microseconds,
//     interval = interval.milliseconds,
//     receiver = fetchTimeAndStateActor,
//     message = FetchTimeAndStateActor.Fetch
//   )
// }
