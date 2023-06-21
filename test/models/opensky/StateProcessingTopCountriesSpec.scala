package models.opensky

import org.scalatest._
import org.scalatestplus.play.PlaySpec
import matchers._
import play.api.test.Helpers._

import spray.json._
import scala.io.Source
import java.time.Instant
import org.checkerframework.checker.units.qual.s
import scala.util.Random

class StateProcessingTopCountriesSpec extends PlaySpec {
  val stateProcessing = new StateProcessing(2)
  val rand = new Random
  import StateJsonProtocol._

  def insertFromFile(file: String) = {
    val source = Source.fromResource(file)
    val str = source.getLines().toList.mkString("\n")
    val jsonAst = str.parseJson
    val states = jsonAst.convertTo[Vector[State]]
    val now = Instant.now.getEpochSecond()

    states.foreach { state =>
      val stateNow = if (rand.nextBoolean()) {
        state.copy(timePosition = Some(now), lastContact = now)
      } else {
        state.copy(timePosition = None, lastContact = now)
      }
      stateProcessing.processState(stateNow)
    }

    val countryCount = stateProcessing.getCountries()
    val countries = countryCount.view.keys.toArray
    (countryCount, countries, states, now)
  }

  "Countries since running" must {
    "states in file netherlands.json" in {
      val (countryCount, countries, states, _) = insertFromFile("netherlands.json")
      assert(countries.length == 25)

      val counts = countryCount.foldLeft(0) { case (acc, (_, counts)) => acc + counts }
      assert(counts == states.length)
    }

    "insert states in file netherlands.json AGAIN" in {
      val (countryCount, countries, states, _) = insertFromFile("netherlands.json")
      assert(countries.length == 25)

      val counts = countryCount.foldLeft(0) { case (acc, (_, counts)) => acc + counts }
      assert(counts == states.length)
    }

    "insert states in file all_1.json" in {
      val (countryCount, countries, states, _) = insertFromFile("all_1.json")
      assert(countries.length > 25)
      val counts = countryCount.foldLeft(0) { case (acc, (_, counts)) => acc + counts }
      val duplicates = 55
      val previousCount = 123
      assert(counts == states.length + previousCount - duplicates)
    }

    "insert states in file all_1.json AGAIN" in {
      val (countryCount, countries, states, _) = insertFromFile("all_1.json")
      assert(countries.length > 25)
      val counts = countryCount.foldLeft(0) { case (acc, (_, counts)) => acc + counts }
      val duplicates = 55
      val previousCount = 123
      assert(counts == states.length + previousCount - duplicates)
    }
  }
}
