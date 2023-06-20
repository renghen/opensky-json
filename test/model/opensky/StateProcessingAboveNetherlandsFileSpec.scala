package model.opensky

import org.scalatest._
import org.scalatestplus.play.PlaySpec
import matchers._
import play.api.test.Helpers._

import spray.json._
import scala.io.Source
import java.time.Instant
import org.checkerframework.checker.units.qual.s
import scala.util.Random
import model.opensky.StateProcessingTest

class StateProcessingAboveNetherlandsFileSpec extends PlaySpec {
  import StateJsonProtocol._

  val stateProcessing = new StateProcessingTest()
  val netherlandsCall = "https://opensky-network.org/api/states/all?lamin=50.75&lomin=3.2&lamax=53.7&lomax=7.22"
  val source = Source.fromResource("netherlands.json")
  val str = source.getLines().toList.mkString("\n")
  val jsonAst = str.parseJson
  val statesInNetherlands = jsonAst.convertTo[Vector[State]]
  val rand = new Random

  def insertAfter1Second() = {
    val now = Instant.now.getEpochSecond()
    statesInNetherlands.foreach { state =>
      val stateNow = if (rand.nextBoolean()) {
        state.copy(timePosition = Some(now), lastContact = now)
      } else {
        state.copy(timePosition = None, lastContact = now)
      }
      stateProcessing.processState(stateNow)
    }
    val aboveNetherlands = stateProcessing.aboveNetherlands()
    val icao24AboveNetherlands = aboveNetherlands.view.keys.toArray
    (aboveNetherlands, icao24AboveNetherlands, now)
  }

  "Test for netherlands in last 2 seconds" must {
    val startedTime = Instant.now.getEpochSecond()

    "states in file now" in {
      val now = startedTime
      statesInNetherlands.foreach { state =>
        val stateNow = state.copy(timePosition = Some(now))
        stateProcessing.processState(stateNow)
      }
      val aboveNetherlands = stateProcessing.aboveNetherlands()
      val icao24AboveNetherlands = aboveNetherlands.view.keys.toArray
      assert(icao24AboveNetherlands.length == statesInNetherlands.length)
      icao24AboveNetherlands.foreach { icao24 =>
        assert(aboveNetherlands(icao24) == now)
      }
    }

    "state in file after 1 sec updated" in {
      Thread.sleep(1000)
      val (aboveNetherlands, icao24AboveNetherlands, now) = insertAfter1Second()
      assert(icao24AboveNetherlands.length == statesInNetherlands.length)
      icao24AboveNetherlands.foreach { icao24 =>
        assert(aboveNetherlands(icao24) == now)
      }
      assert(now == startedTime + 1)
    }

    "state in file after 2 sec updated" in {
      Thread.sleep(1000)
      val (aboveNetherlands, icao24AboveNetherlands, now) = insertAfter1Second()
      assert(icao24AboveNetherlands.length == statesInNetherlands.length)
      icao24AboveNetherlands.foreach { icao24 =>
        assert(aboveNetherlands(icao24) == now)
      }
      assert(now == startedTime + 2)
    }

    "state in file after 3 sec updated" in {
      Thread.sleep(1000)
      val (aboveNetherlands, icao24AboveNetherlands, now) = insertAfter1Second()
      assert(icao24AboveNetherlands.length == statesInNetherlands.length)
      icao24AboveNetherlands.foreach { icao24 =>
        assert(aboveNetherlands(icao24) == now)
      }
      assert(now == startedTime + 3)
    }

    val united = statesInNetherlands.count(pred => pred.originCountry.startsWith("United"))
    val notUnited = statesInNetherlands.filterNot(pred => pred.originCountry.startsWith("United")).toArray
    val notUnitedIcao = notUnited.map(_.icao24)

    "states with country origin(United States, United Kingdom) in file after 1 sec NOT Updated" in {
      Thread.sleep(1000)
      val now = Instant.now.getEpochSecond()
      notUnited.foreach { state =>
        val stateNow = if (rand.nextBoolean()) {
          state.copy(timePosition = Some(now), lastContact = now)
        } else {
          state.copy(timePosition = None, lastContact = now)
        }
        stateProcessing.processState(stateNow)
      }

      val aboveNetherlands = stateProcessing.aboveNetherlands()
      val icao24AboveNetherlands = aboveNetherlands.view.keys.toArray

      assert(icao24AboveNetherlands.length == statesInNetherlands.length)
      icao24AboveNetherlands.foreach { icao24 =>
        if (notUnitedIcao.contains(icao24)) {
          assert(aboveNetherlands(icao24) == now)
        } else {
          assert(aboveNetherlands(icao24) == startedTime + 3)
        }
      }
    }

    "states with country origin(United States, United Kingdom) in file after 2 sec NOT Updated" in {
      Thread.sleep(1000)
      val now = Instant.now.getEpochSecond()
      notUnited.foreach { state =>
        val stateNow = if (rand.nextBoolean()) {
          state.copy(timePosition = Some(now), lastContact = now)
        } else {
          state.copy(timePosition = None, lastContact = now)
        }
        stateProcessing.processState(stateNow)
      }

      val aboveNetherlands = stateProcessing.aboveNetherlands()
      val icao24AboveNetherlands = aboveNetherlands.view.keys.toArray

      assert(icao24AboveNetherlands.length == statesInNetherlands.length)
      icao24AboveNetherlands.foreach { icao24 =>
        if (notUnitedIcao.contains(icao24)) {
          assert(aboveNetherlands(icao24) == now)
        } else {
          assert(aboveNetherlands(icao24) == startedTime + 3)
        }
      }
    }

    "states with country origin(United States, United Kingdom) in file after 3 sec NOT Updated" in {
      Thread.sleep(1000)
      val now = Instant.now.getEpochSecond()
      notUnited.foreach { state =>
        val stateNow = if (rand.nextBoolean()) {
          state.copy(timePosition = Some(now), lastContact = now)
        } else {
          state.copy(timePosition = None, lastContact = now)
        }
        stateProcessing.processState(stateNow)
      }

      val aboveNetherlands = stateProcessing.aboveNetherlands()
      val icao24AboveNetherlands = aboveNetherlands.view.keys.toArray

      assert(icao24AboveNetherlands.length == notUnited.length)
      icao24AboveNetherlands.foreach { icao24 =>
        if (notUnitedIcao.contains(icao24)) {
          assert(aboveNetherlands(icao24) == now)
        }
      }
    }
  }
}
