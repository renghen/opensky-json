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
import scala.util.Random

import java.time.Instant

class StateProcessingTopCountriesSpec extends PlaySpec {
  val rand = new Random
  import StateJsonProtocol._

  val config = Configuration("opensky.top.time" -> 2, "opensky.interval" -> 60_000)
  val fetchTimeAndStateFRP = new FetchTimeAndStateFRP(config)
  implicit val system: ActorSystem = ActorSystem("SingleRequest")

  def insertFromFile(file: String, previous: Vector[CodeCountry]) = {
    val fileSource = io.Source.fromResource(file)
    val str = fileSource.getLines().toList.mkString("\n")
    val jsonAst = str.parseJson
    val states = jsonAst.convertTo[Vector[State]]
    val now = Instant.now.getEpochSecond()

    val source = Source.fromIterator(() => states.iterator)
    val flow = fetchTimeAndStateFRP.insertIntoTopCountries(previous)

    val streamRunning = source
      .map { state =>
        if (rand.nextBoolean()) {
          state.copy(timePosition = Some(now), lastContact = now)
        } else {
          state.copy(timePosition = None, lastContact = now)
        }
      }
      .via(flow)
      .runWith(Sink.seq)

    val lst = await(streamRunning)
    val countryCount = lst.map(_.country).groupMapReduce(identity)(_ => 1) { (acc, el) => acc + el }

    val countries = countryCount.view.keys.toArray
    (countryCount, countries, states, lst, now)
  }

  "Countries since running" must {
    val currentCountries = mutable.ArrayBuffer.empty[CodeCountry]

    "states in file netherlands.json" in {
      val (countryCount, countriesAdded, states, newList, _) =
        insertFromFile("netherlands.json", currentCountries.toVector)
      currentCountries.addAll(newList)

      assert(countriesAdded.length == 25)
      val counts = countryCount.foldLeft(0) { case (acc, (_, counts)) => acc + counts }
      assert(counts == states.length)
    }

    "insert states in file netherlands.json AGAIN" in {
      val (countryCount, countries, states, newList, _) = insertFromFile("netherlands.json", currentCountries.toVector)
      currentCountries.addAll(newList)

      assert(countries.length == 0)
      assert(newList.length == 0)
      assert(currentCountries.length == states.length)
    }

    "insert states in file all_1.json" in {
      val (countryCount, newCountries, states, newList, _) = insertFromFile("all_1.json", currentCountries.toVector)
      currentCountries.addAll(newList)

      assert(states.length == 7128)
      assert(newCountries.length > 25)
      assert(countryCount.size == 100)

      val duplicates = 55
      val previousCount = 123
      assert(currentCountries.length == states.length + previousCount - duplicates)
    }

    "insert states in file all_1.json AGAIN" in {
      val (countryCount, newCountries, states, newList, _) = insertFromFile("all_1.json", currentCountries.toVector)
      currentCountries.addAll(newList)

      assert(newCountries.length == 0)
      assert(newList.length == 0)
      assert(states.length == 7128)
      assert(countryCount.size == 0)

      val duplicates = 55
      val previousCount = 123
      assert(currentCountries.length == states.length + previousCount - duplicates)
    }
  }
}
