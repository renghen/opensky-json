package models.opensky

import play.api.{Configuration, Logger}

import akka.Done
import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorSystem
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.Http
import akka.stream.scaladsl.{Sink, Source}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}
import com.google.inject.ImplementedBy

object FetchTimeAndStateActor {
  final val url = "https://opensky-network.org/api/states/all"
  def props = Props[FetchTimeAndStateActor]()

  case class SayHello(name: String)
  case object Fetch
}

class FetchTimeAndStateActor @Inject() (configuration: Configuration)(implicit
    ec: ExecutionContext
) extends Actor {
  import StateJsonProtocol._
  import FetchTimeAndStateActor._

  val url = configuration.getOptional[String]("opensky.url").getOrElse(FetchTimeAndStateActor.url)
  val delayTime = configuration.getOptional[Int]("opensky.top.time").getOrElse(3600)
  val stateProcessing: StateProcessing = new StateProcessing(delayTime)
  val logger: Logger = Logger(this.getClass())

  implicit val system: ActorSystem = ActorSystem("SingleRequest")
  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  override def receive: Receive = {
    case SayHello(name: String) => {
      logger.info(s"hello, $name")
    }

    case Fetch => {
      logger.info(s"Fetching...")
    }
  }

  def getAirPlanes(): Future[Seq[State]] = {
    import akka.stream.alpakka.json.scaladsl.JsonReader

    val request = Get(url)
    val responseFuture = Http().singleRequest(request)
    stateProcessing.resetStates()
    val unmarshalled = responseFuture.map { response =>
      response.entity.dataBytes
        .via(JsonReader.select("$.states"))
        .mapAsync(1)(bytes => Unmarshal(bytes).to[Seq[State]])
        .mapConcat(identity)
    }
    val source: Source[State, Future[Any]] = Source.futureSource(unmarshalled)
    val result = source.runWith(Sink.foreach { state => stateProcessing.processState(state) })
    result.map(_ => { Seq.empty[State] })
    result.flatMap(_ =>
      Future[Seq[State]] {
        stateProcessing.statesLoaded()
        stateProcessing.getLoadedStates()
      }
    )
  }
}
