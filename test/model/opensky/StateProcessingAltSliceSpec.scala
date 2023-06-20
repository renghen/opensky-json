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

class StateProcessingAltSliceSpec extends PlaySpec {
  val stateProcessing = new StateProcessingTest()
  val rand = new Random
  import StateJsonProtocol._

  def resetAndLoad(str: String) = {
    val jsonAst = str.parseJson
    val states = jsonAst.convertTo[Vector[State]]

    stateProcessing.resetStates()
    states.foreach { state =>
      stateProcessing.processState(state)
    }
    stateProcessing.statesLoaded()
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
      resetAndLoad(str)
      val slices = stateProcessing.getSlices()
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
      resetAndLoad(str)
      val slices = stateProcessing.getSlices()
      assert(slices.keys.toList.length == 1)
      val sortedList = slices.keys.toList.sorted
      assert(sortedList == List(2))
      assert(slices(2).length == 3)
    }

    "states from file netherlands" in {
      val source = Source.fromResource("netherlands.json")
      val str = source.getLines().toList.mkString("\n")
      resetAndLoad(str)

      val slices = stateProcessing.getSlices()
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
    }

  }
}
