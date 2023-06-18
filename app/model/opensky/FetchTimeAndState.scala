package model.opensky

import play.api.Configuration

import akka.actor.ActorSystem
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.Http
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.immutable.HashMap

import javax.inject.{Inject, Singleton}
import akka.stream.scaladsl.{Sink, Source}

import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.RunnableGraph

trait FetchTimeAndState {
  def getAirPlanes(): Future[Seq[State]]
}

object FetchTimeAndStateImpl {
  final val url = "https://opensky-network.org/api/states/all"
}

class FetchTimeAndStateImpl @Inject() (configuration: Configuration)(implicit ec: ExecutionContext)
    extends FetchTimeAndState {
  import StateJsonProtocol._

  val url = configuration.getOptional[String]("opensky.url").getOrElse(FetchTimeAndStateImpl.url)
  implicit val system: ActorSystem = ActorSystem("SingleRequest")
  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  override def getAirPlanes(): Future[Seq[State]] = {
    import akka.stream.alpakka.json.scaladsl.JsonReader

    val request = Get(url)
    val responseFuture = Http().singleRequest(request)
    val unmarshalled = responseFuture.map { response =>
      response.entity.dataBytes
        .via(JsonReader.select("$.states"))
        .mapAsync(1)(bytes => Unmarshal(bytes).to[Seq[State]])
    // .mapConcat(identity)
    }
    val source: Source[Seq[State], Future[Any]] = Source.futureSource(unmarshalled)
    val countries = Flow[Seq[State]].map(lst => toCountries(lst))

    source.runWith(Sink.head)
  }

  def toCountries(lst: Seq[State]) = lst.groupBy(identity).view.mapValues(_.size).toMap

  def isAboveNetherlands(lst: Seq[State]) = lst.filter { state =>
    (state.longitude, state.latitude) match {
      case (Some(long), Some(lat)) => Bounds_WGS84.isAboveNetherlands(long, lat)
      case (_, _)                  => false
    }
  }

}
