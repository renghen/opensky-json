package model.opensky

import org.scalatest._
import org.scalatestplus.play.PlaySpec
import matchers._
import play.api.test.Helpers._

import spray.json._

import collection.mutable.Stack
import org.scalatest._
import matchers._

class StateSpec extends PlaySpec {
  import StateJsonProtocol._

  val str = """
    [
      ["4b1815","SWR341Z ","Switzerland",1686852670,1686852670,20.2612,40.333,11582.4,false,231.61,322.67,0,null,11940.54,"2005",false,0],
      ["a40bd8","N36VK   ","United States",1686852668,1686852668,-94.9346,36.2963,2758.44,false,85.94,54.5,0,null,2827.02,"3054",false,0],
      ["4b1817","EDW215P ","Switzerland",1686852669,1686852669,-14.5886,32.1815,10965.18,false,251.4,33.82,0.33,null,11635.74,null,false,0]
    ]
    """

  "State parsing" must {
    "simple and normal" in {
      val stateStr = """
    ["AAAAAA","12345678","SomeCountry",1000000000,1000000000,10.0,10.0,10000.0,false,200.00,200.0,0,null,10000.00,"2000",false,0]
    """
      val jsonAst = stateStr.parseJson
      val state = jsonAst.convertTo[State]
      assert(state.icao24 == "AAAAAA")
      assert(state.callsign == Some("12345678"))
      assert(state.originCountry == "SomeCountry")
      assert(state.timePosition == Some(1000000000L))
      assert(state.lastContact == 1000000000L)
      assert(state.longitude == Some(10.0))
      assert(state.latitude == Some(10.0))
      assert(state.baroAltitude == Some(10000.0))
      assert(state.onGround == false)
      assert(state.velocity == Some(200.0))
      assert(state.trueTrack == Some(200.0))
      assert(state.verticalRate == Some(0.0))
      assert(state.sensors == None)
      assert(state.geoAltitude == Some(10000.0))
      assert(state.squawk == Some("2000"))
      assert(state.spi == false)
      assert(state.positionSource == PositionSource.ADS_B)
    }

    "sensors is a vector of long" in {
      val stateStr = """
    ["AAAAAA","12345678","SomeCountry",1000000000,1000000000,10.0,10.0,10000.0,false,200.00,200.0,0,[1,2,3,4],10000.00,"2000",false,0]
    """
      val jsonAst = stateStr.parseJson
      val state = jsonAst.convertTo[State]
      assert(state.sensors == Some(List(1, 2, 3, 4)))
    }
  }

  "State writing" must {
    val state = State(
      "AAAAAA",
      Some("12345678"),
      "SomeCountry",
      Some(1000000000L),
      1000000000L,
      Some(10.0),
      Some(10.0),
      Some(10000.0),
      false,
      Some(200.0),
      Some(200.0),
      Some(0.0),
      None,
      Some(10000.0),
      Some("2000"),
      false,
      PositionSource.ADS_B
    )

    "simple and normal" in {
      val resultStr =
        """["AAAAAA","12345678","SomeCountry",1000000000,1000000000,10.0,10.0,10000.0,false,200.0,200.0,0.0,null,10000.0,"2000",false,0]"""      
      assert(state.toJson.toString() == resultStr)
    }

    "sensors is a vector of long" in {
      val newState = state.copy(sensors = Some(List(1,2, 3, 4)))
      val resultStr =
        """["AAAAAA","12345678","SomeCountry",1000000000,1000000000,10.0,10.0,10000.0,false,200.0,200.0,0.0,[1,2,3,4],10000.0,"2000",false,0]"""      
      assert(newState.toJson.toString() == resultStr)
    }

  }

}
