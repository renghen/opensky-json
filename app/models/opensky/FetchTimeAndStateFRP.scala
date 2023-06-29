package models.opensky

import play.api.{Configuration, Logger}

import akka.Done
import akka.actor.{Actor, ActorSystem, Props}

import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.Http
import akka.stream.scaladsl.{Flow, Sink, Source}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.collection.immutable.HashMap

import javax.inject.Inject
import java.time.Instant
import akka.NotUsed

object FetchTimeAndStateFRP {
  type Icao24 = String
  final val url = "https://opensky-network.org/api/states/all"
}

class FetchTimeAndStateFRP @Inject() (configuration: Configuration)(implicit
    ec: ExecutionContext
) {
  import FetchTimeAndStateFRP.Icao24
  import StateJsonProtocol._

  implicit val system: ActorSystem = ActorSystem("SingleRequest")
  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  val logger: Logger = Logger(this.getClass())
  val url = configuration.getOptional[String]("opensky.url").getOrElse(FetchTimeAndStateFRP.url)
  val delayTime = configuration.getOptional[Int]("opensky.top.time").getOrElse(3600)

  logger.info(s"opensky time over Netherlands: $delayTime")

  def getAirPlanes() = {
    import akka.stream.alpakka.json.scaladsl.JsonReader

    val request = Get(url)
    val responseFuture = Http().singleRequest(request)
    val unmarshalled = responseFuture.map { response =>
      response.entity.dataBytes
        .via(JsonReader.select("$.states"))
        .mapAsync(1)(bytes => Unmarshal(bytes).to[Seq[State]])
        .mapConcat(identity)
    }

    val source: Source[State, Future[Any]] = Source.futureSource(unmarshalled)
    // val sink = Sink.head[StateOfFly]
    val flow = slices
    val graph = source.via(flow).runWith(Sink.head) // runWith(Sink.seq)
    graph
  }

  def isAboveNetherlandsForPeriod(
      previousMap: Map[Icao24, Long],
      period: Long
  ): Flow[State, Map[Icao24, Long], NotUsed] = {
    val since = Instant.now().getEpochSecond - period

    Flow[State]
      .filter(isAboveNetherlands)
      .statefulMap(() => previousMap)(
        (hashMap, state) => {
          val newTime = getNewTime(state)
          (hashMap + (state.icao24 -> newTime), hashMap)
        },
        hashMap => Some(hashMap)
      )
      .mapConcat(_.iterator)
      .filter { case (key, value) => value >= since }
      .fold(HashMap.empty[Icao24, Long]) { case (map, el) => map + (el._1 -> el._2) }
  }

  def isAboveNetherlands(state: State): Boolean = {
    (state.longitude, state.latitude) match {
      case (Some(long), Some(lat)) => Bounds_WGS84.isAboveNetherlands(long, lat)
      case (_, _)                  => false
    }
  }

  def getNewTime(state: State): Long = {
    if (state.timePosition.isDefined) {
      state.timePosition.get
    } else {
      state.lastContact
    }
  }

  val slices = Flow[State]
    .map { state =>
      val baroAltitude = state.baroAltitude.getOrElse(0.0)
      val time = state.timePosition.getOrElse(state.lastContact)
      val verticalRate = state.verticalRate.getOrElse(0.0)
      val slicelevel = (baroAltitude / 1000).toLong

      val nextBaroAltitude = baroAltitude + verticalRate
      val nextSlicelevel = (nextBaroAltitude / 1000).toLong

      val flyStatus = if (slicelevel != nextSlicelevel) {
        FlyStatus.WARNING
      } else {
        FlyStatus.NORMAL
      }
      StateOfFly(
        state.icao24,
        state.callsign,
        state.originCountry,
        time,
        state.latitude.getOrElse(0),
        slicelevel,
        baroAltitude,
        verticalRate,
        flyStatus
      )
    }
    .fold(HashMap.empty[Long, List[StateOfFly]]) { case (map, el) =>
      map +
        (el.baroAltitudeSlice -> (map.get(el.baroAltitudeSlice).getOrElse(Nil)).prepended(el))
    }
}
