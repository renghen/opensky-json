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
import akka.stream.{ClosedShape, FlowShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source}
import spray.json._

import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

import javax.inject.Inject
import java.time.Instant

object FetchTimeAndStateFRP {
  type Icao24 = String
  final val url = "https://opensky-network.org/api/states/all"
}

final case class CodeCountry(icao24: String, country: String)

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

  val interval = configuration.getMillis("opensky.interval")
  logger.info(s"opensky scheduler interval $interval")

  private val netherlandsForPeriodFuture: Future[mutable.HashMap[Icao24, Long]] =
    Future.successful(mutable.HashMap.empty[Icao24, Long])

  private val slicesFuture: Future[mutable.HashMap[Long, List[StateOfFly]]] =
    Future.successful(mutable.HashMap.empty[Long, List[StateOfFly]])

  private val listOfCodeCountryFuture: Future[mutable.ArrayBuffer[CodeCountry]] =
    Future.successful(mutable.ArrayBuffer.empty[CodeCountry])

  Source.tick(0.second, interval.millis, "tick").runForeach { _ =>
    for {
      result <- getAirPlanes()
      netherlands = result._1
      newSlices = result._2
      newLstCountryCode = result._3

      nethersPeriod <- netherlandsForPeriodFuture
      _ = nethersPeriod.addAll(netherlands)

      slices <- slicesFuture
      _ = slices.clear()
      _ = slices.addAll(newSlices)

      listOfCodeCountry <- listOfCodeCountryFuture
      _ = listOfCodeCountry.addAll(newLstCountryCode)
    } yield ()
  }

  def overNetherlands(): Future[Int] = netherlandsForPeriodFuture.map(_.toMap.size)

  def getSlices(): Future[Map[Long, List[StateOfFly]]] = slicesFuture.map { _.toMap }

  def topCountries(): Future[Map[String, Int]] = {
    listOfCodeCountryFuture.map { lst =>
      lst.map(_.country).groupMapReduce(identity)(_ => 1) { (acc, el) => acc + el }
    }
  }

  def getAirPlanes() = {
    import akka.stream.alpakka.json.scaladsl.JsonReader
    logger.info(s"fetching airplanes data...")

    val request = Get(url)
    val responseFuture = Http().singleRequest(request)
    val unmarshalled = responseFuture.map { response =>
      response.entity.dataBytes
        .via(JsonReader.select("$.states"))
        .mapAsync(1)(bytes => Unmarshal(bytes).to[Seq[State]])
        .mapConcat(identity)
    }

    for {
      source <- unmarshalled
      netherlandsForPeriod <- netherlandsForPeriodFuture
      listOfCodeCountry <- listOfCodeCountryFuture
      result <- graphBuildingAndRunning(source, netherlandsForPeriod.toMap, listOfCodeCountry.toVector)
    } yield (result)

  }

  def graphBuildingAndRunning(
      source: Source[State, Any],
      netherlandsForPeriod: Map[Icao24, Long],
      icao24Vec: Vector[CodeCountry]
  ): Future[(Map[Icao24, Long], HashMap[Long, List[StateOfFly]], Seq[CodeCountry])] = {
    val netherlandsSink = Sink.head[Map[Icao24, Long]]
    val sliceSink = Sink.head[HashMap[Long, List[StateOfFly]]]
    val countryCodesSink = Sink.seq[CodeCountry]

    val graph = GraphDSL.createGraph(netherlandsSink, sliceSink, countryCodesSink)((_, _, _)) {
      implicit builder => (netherlands, slice, countryCodes) =>
        import GraphDSL.Implicits._ // brings some nice operators in scope
        val input = builder.add(source) // the initial elements

        val slicesFlow: FlowShape[State, HashMap[Long, List[StateOfFly]]] = builder.add(slices)
        val netherlandsFlow: FlowShape[State, Map[Icao24, Long]] =
          builder.add(isAboveNetherlandsForPeriod(netherlandsForPeriod, delayTime))
        val countriesFlow = builder.add(insertIntoTopCountries(icao24Vec))

        val broadcast = builder.add(Broadcast[State](3)) // fan-out operator

        input ~> broadcast
        broadcast.out(0) ~> netherlandsFlow ~> netherlands.in
        broadcast.out(1) ~> slicesFlow ~> slice.in
        broadcast.out(2) ~> countriesFlow ~> countryCodes.in

        ClosedShape
    }

    val result = RunnableGraph.fromGraph(graph).run()

    result._1.zip(result._2).zip(result._3).map { case ((a, b), c) =>
      (a, b, c)
    }
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

  val slices: Flow[State, HashMap[Long, List[StateOfFly]], NotUsed] = Flow[State]
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

  def insertIntoTopCountries(vec: Vector[CodeCountry]): Flow[State, CodeCountry, NotUsed] = Flow[State]
    .filterNot(state => vec.exists(countryCode => countryCode.icao24 == state.icao24))
    .map { state =>
      CodeCountry(state.icao24, state.originCountry)
    }
}
