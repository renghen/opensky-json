package model.opensky

import play.api.Configuration

import akka.actor.ActorSystem
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.common.JsonEntityStreamingSupport
// import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.Http
import spray.json._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

import javax.inject.Inject
import javax.inject.Singleton
import akka.stream.scaladsl.Source
import akka.NotUsed
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.JsonFraming

trait FetchTimeAndState {
  def getAirPlanes(): Future[TimeAndStates]
}

object FetchTimeAndStateImpl {
  final val url = "https://opensky-network.org/api/states/all"
}

class FetchTimeAndStateImpl @Inject() (configuration: Configuration)(implicit ec: ExecutionContext)
    extends FetchTimeAndState {
  val url = configuration.getOptional[String]("opensky.url").getOrElse(FetchTimeAndStateImpl.url)
  implicit val system: ActorSystem = ActorSystem("SingleRequest")
  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()
  import TimeAndStatesJsonProtocol._

  override def getAirPlanes(): Future[TimeAndStates] = {
    val request = Get(url)
    val responseFuture = Http().singleRequest(request)
    val unmarshalled: Future[Source[TimeAndStates, Any]] = responseFuture.map { response =>
      response.entity.dataBytes
        .via(JsonFraming.objectScanner(2_048_000))
        .mapAsync(1)(bytes => Unmarshal(bytes).to[TimeAndStates])
    }
    val source = Source.futureSource(unmarshalled)
    source.runWith(Sink.head)
  }
}
