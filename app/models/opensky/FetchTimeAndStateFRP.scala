package models.opensky

import play.api.{Configuration, Logger}

import akka.Done
import akka.NotUsed
import akka.actor.{Actor, ActorSystem, Props}

import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.Http
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source, Zip}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.collection.immutable.HashMap
import scala.collection.mutable

import javax.inject.Inject
import java.time.Instant
import akka.stream.scaladsl.RunnableGraph
import akka.stream.FlowShape

object FetchTimeAndStateFRP {
  type Icao24 = String
  final val url = "https://opensky-network.org/api/states/all"

  // def props = Props[FetchTimeAndStateActor]()
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

  private val netherlandsForPeriodFuture: Future[mutable.HashMap[Icao24, Long]] =
    Future.successful(mutable.HashMap.empty[Icao24, Long])
  private val slicesFuture: Future[mutable.HashMap[Long, List[StateOfFly]]] =
    Future.successful(mutable.HashMap.empty[Long, List[StateOfFly]])

  logger.info(s"opensky time over Netherlands: $delayTime")

  Source.tick(0.second, 1.minute, getAirPlanes()).runForeach { result =>
    logger.info(s"fetching...")

    val netherlands: Future[Map[Icao24, Long]] = result._1
    netherlands.flatMap { map =>
      netherlandsForPeriodFuture.map { oriMap => oriMap.addAll(map) }
    }

    val slices: Future[HashMap[Long, List[StateOfFly]]] = result._2
    slices.flatMap { sl =>
      slicesFuture.map { re =>
        re.clear(); re.addAll(sl)
      }
    }
  }

  def overNetherlands(): Future[Int] = netherlandsForPeriodFuture.map(_.toMap.size)

  def getSlices(): Future[Map[Long, List[StateOfFly]]] = slicesFuture.map { _.toMap }

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
    val netherlandsSink = Sink.head[Map[Icao24, Long]]
    val sliceSink = Sink.head[HashMap[Long, List[StateOfFly]]]

    val graph = GraphDSL.createGraph(netherlandsSink, sliceSink)((_, _)) { implicit builder => (netherlands, slice) =>
      import GraphDSL.Implicits._ // brings some nice operators in scope
      val input = builder.add(source) // the initial elements

      val slicesFlow: FlowShape[State, HashMap[Long, List[StateOfFly]]] = builder.add(slices())
      val netherlandsFlow: FlowShape[State, Map[Icao24, Long]] =
        builder.add(isAboveNetherlandsForPeriod(Map.empty[Icao24, Long], delayTime))

      val broadcast = builder.add(Broadcast[State](2)) // fan-out operator

      input ~> broadcast
      broadcast.out(0) ~> netherlandsFlow ~> netherlands.in
      broadcast.out(1) ~> slicesFlow ~> slice.in

      ClosedShape
    }

    RunnableGraph.fromGraph(graph).run()
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

  def slices(): Flow[State, HashMap[Long, List[StateOfFly]], NotUsed] = Flow[State]
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
