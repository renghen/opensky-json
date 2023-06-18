package model.opensky

import play.api.Configuration

import akka.actor.ActorSystem
import akka.http.scaladsl.unmarshalling.Unmarshal
// import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.Http
import spray.json._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

import javax.inject.Inject

trait FetchTimeAndState {
  def getAirPlanes(): Future[String]
}

object FetchTimeAndState {
  final val url = "https://opensky-network.org/api/states/all"
}

class FetchTimeAndStateImpl @Inject() (configuration: Configuration)(implicit ec: ExecutionContext)
    extends FetchTimeAndState {
  val url = configuration.getOptional[String]("opensky.url").getOrElse(FetchTimeAndState.url)

  implicit val system: ActorSystem = ActorSystem("SingleRequest")

  def getAirPlanes() = {
    val request = Get(url)
    val responseFuture: Future[HttpResponse] = Http().singleRequest(request)
    val result = responseFuture.flatMap { response =>
      val source = Unmarshal(response.entity).to[String]
      source
    }
    result
  }
}
