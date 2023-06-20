package model.opensky

import org.scalatest._
import org.scalatestplus.play.PlaySpec
import matchers._
import play.api.test.Helpers._

import spray.json._
import scala.io.Source

class StateProcessingTest extends StateProcessing {
  final val delay: Int = 2
}

class StateProcessingSpec extends PlaySpec {

  val stateProcessing = new StateProcessingTest()

  "State Processing set up" must {
    "delay is 2 seconds" in {
      assert(stateProcessing.delay == 2)
    }
  }

  "Test for netherlands" must {
    val state = State(
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
    "state with coordinates: (4.7712, 51.0)" in {
      assert(stateProcessing.isAboveNetherlands(state))
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
}
