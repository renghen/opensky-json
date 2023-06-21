package models.opensky

import play.api.{Configuration, Logger}

import akka.Done
import akka.actor.{Actor, ActorSystem, Props}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.Http
import akka.stream.scaladsl.{Sink, Source}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

object FetchTimeAndStateActor {
  final val url = "https://opensky-network.org/api/states/all"

  def props = Props[FetchTimeAndStateActor]()

  case object Fetch
  case object OverNetherlands
  case object TopCountries
  case object GetSlices
}

class FetchTimeAndStateActor @Inject() (configuration: Configuration)(implicit
    ec: ExecutionContext
) extends Actor {
  val logger: Logger = Logger(this.getClass())

  import StateJsonProtocol._
  import FetchTimeAndStateActor._

  val url = configuration.getOptional[String]("opensky.url").getOrElse(FetchTimeAndStateActor.url)
  val delayTime = configuration.getOptional[Int]("opensky.top.time").getOrElse(3600)
  val stateProcessing: StateProcessing = new StateProcessing(delayTime)

  implicit val system: ActorSystem = ActorSystem("SingleRequest")
  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  override def receive: Receive = {
    case OverNetherlands => {
      logger.info(s"Over Netherlands for past hour")
      stateProcessing.aboveNetherlands().size
    }

    case TopCountries => {
      logger.info(s"TopCountries since running...")
      val countries = stateProcessing.getCountries()
      sender() ! countries
    }

    case Fetch => {
      logger.info(s"Fetching from opensky...")
      getAirPlanes().flatMap(_ => Future.successful(stateProcessing.statesLoaded()))
    }
  }

  def getAirPlanes(): Future[Done] = {
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
    source.runWith(Sink.foreach { state => stateProcessing.processState(state) })
  }
}
