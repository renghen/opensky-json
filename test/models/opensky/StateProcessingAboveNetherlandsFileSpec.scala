package models.opensky

import org.scalatest._
import org.scalatestplus.play.PlaySpec
import matchers._
import play.api.test.Helpers._
import play.api.Configuration

import java.time.Instant

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import spray.json._

import scala.io
import scala.collection.mutable
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

class StateProcessingAboveNetherlandsFileSpec extends PlaySpec {
  import StateJsonProtocol._

  val fileSource = io.Source.fromResource("netherlands.json")
  val str = fileSource.getLines().toList.mkString("\n")
  val jsonAst = str.parseJson
  val statesInNetherlands = jsonAst.convertTo[Vector[State]]
  val source = Source
    .fromIterator(() => statesInNetherlands.iterator)

  val rand = new Random()
  val config = Configuration("opensky.top.time" -> 2)
  val fetchTimeAndStateFRP = new FetchTimeAndStateFRP(config)

  implicit val system: ActorSystem = ActorSystem("SingleRequest")

  def processOverNetherlands(source: Source[State, NotUsed], previous: Map[FetchTimeAndStateFRP.Icao24, Long]) = {
    val now = Instant.now.getEpochSecond()

    val flow = fetchTimeAndStateFRP.isAboveNetherlandsForPeriod(previous, 2)
    val streamRunning = source
      .map { state =>
        if (rand.nextBoolean()) {
          state.copy(timePosition = Some(now), lastContact = now)
        } else {
          state.copy(timePosition = None, lastContact = now)
        }
      }
      .via(flow)
      .runWith(Sink.head)

    val aboveNetherlands = await(streamRunning)
    val icao24AboveNetherlands = aboveNetherlands.view.keys.toArray
    (aboveNetherlands, icao24AboveNetherlands, now)
  }

  "Test for netherlands in last 2 seconds" must {
    val startedTime = Instant.now.getEpochSecond()
    val overNetherlands = mutable.HashMap.empty[FetchTimeAndStateFRP.Icao24, Long]

    "states in file now" in {
      val (aboveNetherlands, icao24AboveNetherlands, now) = processOverNetherlands(source, overNetherlands.toMap)
      overNetherlands.addAll(aboveNetherlands)

      assert(icao24AboveNetherlands.length == statesInNetherlands.length)
      icao24AboveNetherlands.foreach { icao24 =>
        assert(aboveNetherlands(icao24) == now)
      }
    }

    "state in file after 1 sec updated" in {
      Thread.sleep(1000)
      val (aboveNetherlands, icao24AboveNetherlands, now) = processOverNetherlands(source, overNetherlands.toMap)
      overNetherlands.addAll(aboveNetherlands)

      assert(icao24AboveNetherlands.length == statesInNetherlands.length)
      icao24AboveNetherlands.foreach { icao24 =>
        assert(aboveNetherlands(icao24) == now)
      }
      assert(now >= startedTime + 1)
    }

    "state in file after 2 sec updated" in {
      Thread.sleep(1000)
      val (aboveNetherlands, icao24AboveNetherlands, now) = processOverNetherlands(source, overNetherlands.toMap)
      overNetherlands.addAll(aboveNetherlands)

      assert(icao24AboveNetherlands.length == statesInNetherlands.length)
      icao24AboveNetherlands.foreach { icao24 =>
        assert(aboveNetherlands(icao24) == now)
      }
      assert(now >= startedTime + 2)
    }

    "state in file after 3 sec updated" in {
      Thread.sleep(1000)
      val (aboveNetherlands, icao24AboveNetherlands, now) = processOverNetherlands(source, overNetherlands.toMap)
      overNetherlands.addAll(aboveNetherlands)

      assert(icao24AboveNetherlands.length == statesInNetherlands.length)
      icao24AboveNetherlands.foreach { icao24 =>
        assert(aboveNetherlands(icao24) == now)
      }
      assert(now >= startedTime + 3)
    }

    val united = statesInNetherlands.count(pred => pred.originCountry.startsWith("United"))
    val notUnited = statesInNetherlands.filterNot(pred => pred.originCountry.startsWith("United")).toArray
    val notUnitedIcao = notUnited.map(_.icao24)
    val notUnitedSource = Source.fromIterator(() => notUnited.iterator)

    "states with country origin(United States, United Kingdom) in file after 1 sec NOT Updated" in {
      Thread.sleep(1000)
      val (aboveNetherlands, icao24AboveNetherlands, now) =
        processOverNetherlands(notUnitedSource, overNetherlands.toMap)
      overNetherlands.addAll(aboveNetherlands)

      assert(icao24AboveNetherlands.length == statesInNetherlands.length)
      icao24AboveNetherlands.foreach { icao24 =>
        if (notUnitedIcao.contains(icao24)) {
          assert(aboveNetherlands(icao24) == now)
        } else {
          assert(aboveNetherlands(icao24) >= startedTime + 3)
        }
      }
    }

    "states with country origin(United States, United Kingdom) in file after 2 sec NOT Updated" in {
      Thread.sleep(1000)
      val (aboveNetherlands, icao24AboveNetherlands, now) =
        processOverNetherlands(notUnitedSource, overNetherlands.toMap)
      overNetherlands.addAll(aboveNetherlands)

      assert(icao24AboveNetherlands.length == statesInNetherlands.length)
      icao24AboveNetherlands.foreach { icao24 =>
        if (notUnitedIcao.contains(icao24)) {
          assert(aboveNetherlands(icao24) == now)
        } else {
          assert(aboveNetherlands(icao24) >= startedTime + 3)
        }
      }
    }

    "states with country origin(United States, United Kingdom) in file after 3 sec NOT Updated" in {
      Thread.sleep(1000)
      val (aboveNetherlands, icao24AboveNetherlands, now) =
        processOverNetherlands(notUnitedSource, overNetherlands.toMap)
      overNetherlands.addAll(aboveNetherlands)

      assert(icao24AboveNetherlands.length == notUnited.length)
      icao24AboveNetherlands.foreach { icao24 =>
        if (notUnitedIcao.contains(icao24)) {
          assert(aboveNetherlands(icao24) == now)
        }
      }
    }
  }
}
