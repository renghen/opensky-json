package models.opensky

import org.scalatest._
import org.scalatestplus.play.PlaySpec
import matchers._
import play.api.test.Helpers._

import spray.json._

import org.scalatest._
import matchers._

class TimeAndStatesSpec extends PlaySpec {
  import TimeAndStatesJsonProtocol._

  val timeAndStatestr = """
  {
    "time":1686914920,
    "states":[
        ["4b1815","SWR7EA  ","Switzerland",1686914919,1686914919,2.2629,49.8882,11574.78,false,180.61,317.54,-0.33,null,11879.58,"3040",false,0],
        ["4b1816","SWR590N ","Switzerland",1686914918,1686914919,4.6719,43.0242,11285.22,false,233.83,208.09,0,null,11765.28,"3044",false,0],
        ["4b1817","SWR79W  ","Switzerland",1686914919,1686914919,5.5217,44.22,11277.6,false,237.9,200.9,0,null,11696.7,"3031",false,0],
        ["511141","ESETI   ","Estonia",1686914918,1686914918,26.1485,58.2547,1584.96,false,83.85,150.6,-2.28,null,1668.78,null,false,0],
        ["ab1644","UAL1304 ","United States",1686914919,1686914919,-74.3033,40.641,1600.2,false,133.76,269.34,12.35,null,1554.48,"2607",false,0]
    ]
  }    
  """

  "Time and States parsing" must {
    "simple and normal" in {
      val jsonAst = timeAndStatestr.parseJson
      val result = jsonAst.convertTo[TimeAndStates]
      assert(result.time == 1686914920)
      assert(result.states.length == 5)
      assert(result.states.count(st => st.positionSource == PositionSource.ADS_B) == 5)
      assert(result.states.count(st => st.positionSource == PositionSource.ASTERIX) == 0)
      assert(result.states.count(st => st.originCountry == "Switzerland") == 3)
      assert(result.states.count(st => st.originCountry == "Estonia") == 1)
      assert(result.states.count(st => st.originCountry == "United States") == 1)
    }
  }
}
