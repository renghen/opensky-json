package model.opensky

import org.scalatest._
import org.scalatestplus.play.PlaySpec
import matchers._
import play.api.test.Helpers._

import spray.json._
import scala.io.Source
import java.time.Instant
import org.checkerframework.checker.units.qual.s

class StateProcessingTest extends StateProcessing {
  final val delay: Int = 2
}

class StateProcessingSpec extends PlaySpec {

  val stateProcessing = new StateProcessingTest()

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
      assert(stateProcessing.delay == 2)
    }
  }

  "Test for netherlands" must {

    "state with coordinates: (4.7712, 51.0)" in {
      assert(stateProcessing.isAboveNetherlands(stateInNetherlands))
    }

    val netherlandsCall = "https://opensky-network.org/api/states/all?lamin=50.75&lomin=3.2&lamax=53.7&lomax=7.22"
    s"states from url $netherlandsCall in file" in {
      val source = Source.fromResource("netherlands.json")
      val str = source.getLines().toList.mkString("\n")
      import StateJsonProtocol._
      val jsonAst = str.parseJson
      val states = jsonAst.convertTo[Vector[State]]
      assert(states.length == 123)
      assert(states.filter(stateProcessing.isAboveNetherlands).length == 123)
    }
  }

  def insertAfterSecond() = {
    val now = Instant.now.getEpochSecond()
    val stateInNetherlandsNow = stateInNetherlands.copy(timePosition = Some(now))
    stateProcessing.processState(stateInNetherlandsNow)

    val aboveNetherlands = stateProcessing.aboveNetherlands()
    val icao24AboveNetherlands = aboveNetherlands.view.keys.toArray
    (aboveNetherlands, icao24AboveNetherlands, now)
  }

  "Test for netherlands in last 2 seconds" must {
    val startedTime = Instant.now.getEpochSecond()
    "state with coordinates: (4.7712, 51.0) now" in {
      val now = startedTime
      val stateInNetherlandsNow = stateInNetherlands.copy(timePosition = Some(now))
      stateProcessing.processState(stateInNetherlandsNow)
      assert(stateProcessing.aboveNetherlands().view.keys.toArray.length == 1)
    }

    "state with coordinates: (4.7712, 51.0) after 1 sec updated" in {
      Thread.sleep(1000)
      val (aboveNetherlands, icao24AboveNetherlands, now) = insertAfterSecond()
      assert(icao24AboveNetherlands.length == 1)
      val icao24 = icao24AboveNetherlands(0)
      assert(icao24AboveNetherlands(0) == stateInNetherlands.icao24)
      assert(aboveNetherlands(icao24) == now)
      assert(now == startedTime + 1)
    }

    "state with coordinates: (4.7712, 51.0) after 2 sec updated" in {
      Thread.sleep(1000)
      val (aboveNetherlands, icao24AboveNetherlands, now) = insertAfterSecond()
      assert(icao24AboveNetherlands.length == 1)
      val icao24 = icao24AboveNetherlands(0)
      assert(icao24AboveNetherlands(0) == stateInNetherlands.icao24)
      assert(aboveNetherlands(icao24) == now)
      assert(now == startedTime + 2)
    }

    "state with coordinates: (4.7712, 51.0) after 3 sec updated" in {
      Thread.sleep(1000)
      val (aboveNetherlands, icao24AboveNetherlands, now) = insertAfterSecond()
      assert(icao24AboveNetherlands.length == 1)
      val icao24 = icao24AboveNetherlands(0)
      assert(icao24AboveNetherlands(0) == stateInNetherlands.icao24)
      assert(aboveNetherlands(icao24) == now)
      assert(now == startedTime + 3)
    }

    "state with coordinates: (4.7712, 51.0) after 1 sec NOT Updated" in {
      Thread.sleep(1000)
      val now = Instant.now.getEpochSecond()
      val aboveNetherlands = stateProcessing.aboveNetherlands()
      val icao24AboveNetherlands = aboveNetherlands.view.keys.toArray

      assert(icao24AboveNetherlands.length == 1)
      val icao24 = icao24AboveNetherlands(0)
      assert(icao24AboveNetherlands(0) == stateInNetherlands.icao24)
      assert(aboveNetherlands(icao24) == startedTime + 3)
      assert(now == startedTime + 4)
    }

    "state with coordinates: (4.7712, 51.0) after 2 sec NOT Updated" in {
      Thread.sleep(1000)
      val now = Instant.now.getEpochSecond()
      val aboveNetherlands = stateProcessing.aboveNetherlands()
      val icao24AboveNetherlands = aboveNetherlands.view.keys.toArray

      assert(icao24AboveNetherlands.length == 1)
      val icao24 = icao24AboveNetherlands(0)
      assert(icao24AboveNetherlands(0) == stateInNetherlands.icao24)
      assert(aboveNetherlands(icao24) == startedTime + 3)
      assert(now == startedTime + 5)
    }

    "state with coordinates: (4.7712, 51.0) after 3 sec NOT Updated" in {
      Thread.sleep(1000)
      val now = Instant.now.getEpochSecond()
      val aboveNetherlands = stateProcessing.aboveNetherlands()
      val icao24AboveNetherlands = aboveNetherlands.view.keys.toArray

      assert(icao24AboveNetherlands.length == 0)
      assert(now == startedTime + 6)
    }

//     val netherlandsCall = "https://opensky-network.org/api/states/all?lamin=50.75&lomin=3.2&lamax=53.7&lomax=7.22"
//     s"states from url $netherlandsCall in file" in {
//       val source = Source.fromResource("netherlands.json")
//       val str = source.getLines().toList.mkString("\n")
//       import StateJsonProtocol._
//       val jsonAst = str.parseJson
//       val states = jsonAst.convertTo[Vector[State]]
//       assert(states.length == 123)
//       assert(states.filter(stateProcessing.isAboveNetherlands).length == 123)
//     }
//   }

  }
}
