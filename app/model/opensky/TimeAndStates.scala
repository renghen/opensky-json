package model.opensky
import spray.json._

final case class TimeAndStates(
    time: Long,
    states: List[State]
)

object TimeAndStateJsonProtocol extends DefaultJsonProtocol {
  import StateJsonProtocol._

  implicit val timeAndStatesFormat: RootJsonFormat[TimeAndStates] = jsonFormat2(TimeAndStates.apply)
}