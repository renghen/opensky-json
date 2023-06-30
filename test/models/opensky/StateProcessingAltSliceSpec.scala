package models.opensky

import org.scalatest._
import org.scalatestplus.play.PlaySpec
import matchers._
import play.api.test.Helpers._
import play.api.Configuration

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io
import scala.util.Random

import java.time.Instant

class StateProcessingAltSliceSpec extends PlaySpec {
  val rand = new Random
  val config = Configuration("opensky.top.time" -> 2)
  val fetchTimeAndStateFRP = new FetchTimeAndStateFRP(config)
  val flow = fetchTimeAndStateFRP.slices

  implicit val system: ActorSystem = ActorSystem("SingleRequest")
  import StateJsonProtocol._

  def loadFromStream(str: String)(transform: State => State) = {
    val jsonAst = str.parseJson
    val states = jsonAst.convertTo[Vector[State]]
    val source = Source
      .fromIterator(() => states.iterator)
      .map(transform)
    val resultFuture = source.via(flow).runWith(Sink.head)
    await(resultFuture)
  }

  "slice" must {
    "states loaded test" in {
      val str = """
        [
          ["4b1815","SWR341Z ","Switzerland",1686852670,1686852670,20.2612,40.333,11582.4,false,231.61,322.67,0,null,11940.54,"2005",false,0],
          ["a40bd8","N36VK   ","United States",1686852668,1686852668,-94.9346,36.2963,2758.44,false,85.94,54.5,0,null,2827.02,"3054",false,0],
          ["4b1817","EDW215P ","Switzerland",1686852669,1686852669,-14.5886,32.1815,10965.18,false,251.4,33.82,0.33,null,11635.74,null,false,0]
        ]
      """
      val slices = loadFromStream(str)(identity)
      assert(slices.size == 3)
      val sortedList = slices.keys.toList.sorted
      assert(sortedList == List(2, 10, 11))
    }

    "states to have one slice with 3 elements in slice" in {
      val str = """
        [
          ["4b1815","SWR341Z ","Switzerland",1686852670,1686852670,20.2612,40.333,2582.4,false,231.61,322.67,0,null,11940.54,"2005",false,0],
          ["a40bd8","N36VK   ","United States",1686852668,1686852668,-94.9346,36.2963,2758.44,false,85.94,54.5,0,null,2827.02,"3054",false,0],
          ["4b1817","EDW215P ","Switzerland",1686852669,1686852669,-14.5886,32.1815,2965.18,false,251.4,33.82,0.33,null,11635.74,null,false,0]
        ]
      """
      val slices = loadFromStream(str)(identity)
      assert(slices.keys.toList.length == 1)

      val sortedList = slices.keys.toList.sorted
      assert(sortedList == List(2))
      assert(slices(2).length == 3)
    }

    val source = io.Source.fromResource("netherlands.json")
    val netherlandsStr = source.getLines().toList.mkString("\n")
    "states from file netherlands" in {
      val slices = loadFromStream(netherlandsStr)(identity)

      assert(slices.keys.toList.length == 11)

      val sortedList = slices.keys.toList.sorted
      assert(sortedList == List(0, 1, 2, 3, 4, 5, 6, 7, 9, 10, 11))

      val slice1 = slices(1)
      assert(slice1.length == 4)

      val listValues = sortedList.map(slices)
      val totalStates = listValues.flatten.length
      assert(totalStates == 123)

      val listCount = listValues.map(_.length)
      assert(listCount.sum == 123)

      val grpFlyStatus = listValues.flatten.groupBy(_.status)
      assert(grpFlyStatus(FlyStatus.NORMAL).length == 123)
    }

    "states from file netherlands with US & UK change slice" in {
      val slices = loadFromStream(netherlandsStr) { state: State =>
        if (state.originCountry.contains("United")) {
          state.copy(verticalRate = Some(-1000.0), baroAltitude = state.baroAltitude.map(_ + 1000))
        } else {
          state
        }
      }

      val sortedList = slices.keys.toList.sorted
      val listValues = sortedList.map(slices)
      val totalStates = listValues.flatten.length
      assert(totalStates == 123)

      val grpFlyStatus = listValues.flatten.groupBy(_.status)
      assert(grpFlyStatus(FlyStatus.WARNING).length == 25)
      assert(grpFlyStatus(FlyStatus.NORMAL).length == 123 - 25)
    }
  }
}
