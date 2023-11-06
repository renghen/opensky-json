package models.opensky

import org.scalatest._
import org.scalatestplus.play.PlaySpec
import matchers._
import play.api.test.Helpers._
import play.api.Configuration

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import spray.json._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io
import java.time.Instant

class StateProcessingAboveNetherlandsSpec extends PlaySpec {
  val config = Configuration("opensky.top.time" -> 2, "opensky.interval" -> 60_000)
  val fetchTimeAndStateFRP = new FetchTimeAndStateFRP(config)
  implicit val system: ActorSystem = ActorSystem("SingleRequest")

  val stateInNetherlands = State(
    "aa441c",
    Some("UAL20   "),
    "United States",
    Some(1687245490L),
    1687245490L,
    Some(4.7712),
    Some(51.0),
    None,
    true,
    Some(0.0),
    Some(182.81),
    Some(0.0),
    None,
    None,
    Some("6253"),
    false,
    PositionSource.ADS_B
  )

  "State Processing set up" must {
    "delay is 2 seconds" in {
      assert(fetchTimeAndStateFRP.delayTime == 2)
    }
  }

  "Test for netherlands" must {

    "state with coordinates: (4.7712, 51.0)" in {
      assert(fetchTimeAndStateFRP.isAboveNetherlands(stateInNetherlands))
    }

    val netherlandsCall = "https://opensky-network.org/api/states/all?lamin=50.75&lomin=3.2&lamax=53.7&lomax=7.22"
    s"states from url $netherlandsCall in file" in {
      import StateJsonProtocol._

      val source = io.Source.fromResource("netherlands.json")
      val str = source.getLines().toList.mkString("\n")
      val jsonAst = str.parseJson
      val states = jsonAst.convertTo[Vector[State]]

      assert(states.length == 123)
      assert(states.filter(fetchTimeAndStateFRP.isAboveNetherlands).length == 123)
    }
  }

  def insertAfterSecond(state: State, previous: Map[FetchTimeAndStateFRP.Icao24, Long]) = {
    val source: Source[State, NotUsed] = Source.single(state)
    val flow = fetchTimeAndStateFRP.isAboveNetherlandsForPeriod(previous, 2)
    val streamRunning = source
      .via(flow)
      .runWith(Sink.head)

    val aboveNetherlands = await(streamRunning)
    val icao24AboveNetherlands = aboveNetherlands.view.keys.toArray
    (aboveNetherlands, icao24AboveNetherlands, state.timePosition.get)
  }

  "Test for netherlands in last 2 seconds" must {
    val startedTime = Instant.now.getEpochSecond()
    val overNetherlands = mutable.HashMap.empty[FetchTimeAndStateFRP.Icao24, Long]

    "state with coordinates: (4.7712, 51.0) now" in {
      val stateInNetherlandsNow = stateInNetherlands.copy(timePosition = Some(startedTime))
      val (aboveNetherlands, icao24AboveNetherlands, now) =
        insertAfterSecond(stateInNetherlandsNow, overNetherlands.toMap)
      overNetherlands.addAll(aboveNetherlands)

      assert(aboveNetherlands.view.keys.toArray.length == 1)
    }

    "state with coordinates: (4.7712, 51.0) after 1 sec updated" in {
      Thread.sleep(1000)
      val now = Instant.now.getEpochSecond()
      val stateInNetherlandsNow = stateInNetherlands.copy(timePosition = Some(now))
      val (aboveNetherlands, icao24AboveNetherlands, returnTime) =
        insertAfterSecond(stateInNetherlandsNow, overNetherlands.toMap)
      overNetherlands.addAll(aboveNetherlands)

      assert(icao24AboveNetherlands.length == 1)
      val icao24 = icao24AboveNetherlands(0)
      assert(icao24AboveNetherlands(0) == stateInNetherlands.icao24)
      assert(aboveNetherlands(icao24) == now)
      assert(returnTime >= startedTime + 1)
    }

    "state with coordinates: (4.7712, 51.0) after 2 sec updated" in {
      Thread.sleep(1000)
      val now = Instant.now.getEpochSecond()
      val stateInNetherlandsNow = stateInNetherlands.copy(timePosition = Some(now))
      val (aboveNetherlands, icao24AboveNetherlands, returnTime) =
        insertAfterSecond(stateInNetherlandsNow, overNetherlands.toMap)
      overNetherlands.addAll(aboveNetherlands)

      assert(icao24AboveNetherlands.length == 1)
      val icao24 = icao24AboveNetherlands(0)
      assert(icao24AboveNetherlands(0) == stateInNetherlands.icao24)
      assert(aboveNetherlands(icao24) == now)
      assert(now >= startedTime + 2)
    }

    "state with coordinates: (4.7712, 51.0) after 3 sec updated" in {
      Thread.sleep(1000)
      val now = Instant.now.getEpochSecond()
      val stateInNetherlandsNow = stateInNetherlands.copy(timePosition = Some(now))
      val (aboveNetherlands, icao24AboveNetherlands, returnTime) =
        insertAfterSecond(stateInNetherlandsNow, overNetherlands.toMap)
      overNetherlands.addAll(aboveNetherlands)

      assert(icao24AboveNetherlands.length == 1)
      val icao24 = icao24AboveNetherlands(0)
      assert(icao24AboveNetherlands(0) == stateInNetherlands.icao24)
      assert(aboveNetherlands(icao24) == now)
      assert(now >= startedTime + 3)
    }

    "state with coordinates: (4.7712, 51.0) after 1 sec NOT Updated" in {
      Thread.sleep(1000)
      val now = Instant.now.getEpochSecond() - 1
      val stateInNetherlandsNow = stateInNetherlands.copy(timePosition = Some(now))
      val (aboveNetherlands, icao24AboveNetherlands, returnTime) =
        insertAfterSecond(stateInNetherlandsNow, overNetherlands.toMap)
      overNetherlands.addAll(aboveNetherlands)

      assert(icao24AboveNetherlands.length == 1)
      val icao24 = icao24AboveNetherlands(0)
      assert(icao24AboveNetherlands(0) == stateInNetherlands.icao24)
      assert(aboveNetherlands(icao24) >= startedTime + 3)
      assert(returnTime >= startedTime + 3)
    }

    "state with coordinates: (4.7712, 51.0) after 2 sec NOT Updated" in {
      Thread.sleep(1000)
      val now = Instant.now.getEpochSecond() - 2
      val stateInNetherlandsNow = stateInNetherlands.copy(timePosition = Some(now))
      val (aboveNetherlands, icao24AboveNetherlands, returnTime) =
        insertAfterSecond(stateInNetherlandsNow, overNetherlands.toMap)
      overNetherlands.addAll(aboveNetherlands)

      assert(icao24AboveNetherlands.length == 1)
      val icao24 = icao24AboveNetherlands(0)
      assert(icao24AboveNetherlands(0) == stateInNetherlands.icao24)
      assert(aboveNetherlands(icao24) >= startedTime + 3)
      assert(returnTime >= startedTime + 3)
    }

    "state with coordinates: (4.7712, 51.0) after 3 sec NOT Updated" in {
      Thread.sleep(1000)
      val now = Instant.now.getEpochSecond() - 3
      val stateInNetherlandsNow = stateInNetherlands.copy(timePosition = Some(now))
      val (aboveNetherlands, icao24AboveNetherlands, returnTime) =
        insertAfterSecond(stateInNetherlandsNow, overNetherlands.toMap)
      overNetherlands.addAll(aboveNetherlands)

      assert(icao24AboveNetherlands.length == 0)
      assert(returnTime >= startedTime + 3)
    }

    "state with coordinates: (4.7712, 51.0) comes again" in {
      val now = Instant.now.getEpochSecond()
      val stateInNetherlandsNow = stateInNetherlands.copy(timePosition = Some(now))
      val (aboveNetherlands, icao24AboveNetherlands, returnTime) =
        insertAfterSecond(stateInNetherlandsNow, overNetherlands.toMap)
      overNetherlands.addAll(aboveNetherlands)

      assert(aboveNetherlands.view.keys.toArray.length == 1)
    }
  }
}
