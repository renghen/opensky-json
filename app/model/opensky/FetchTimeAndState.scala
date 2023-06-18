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
import akka.util.ByteString
import akka.stream.scaladsl.Flow

trait FetchTimeAndState {
  def getAirPlanes(): Future[Seq[State]]
}

object FetchTimeAndStateImpl {
  final val url = "https://opensky-network.org/api/states/all"
}

class FetchTimeAndStateImpl @Inject() (configuration: Configuration)(implicit ec: ExecutionContext)
    extends FetchTimeAndState {

  // import TimeAndStatesJsonProtocol._
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
    }
    val source: Source[Seq[State], Future[Any]] = Source.futureSource(unmarshalled)
    source.runWith(Sink.head)
  }
}
