package model.opensky

import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

import enumeratum._

sealed trait FlyStatus extends EnumEntry

object FlyStatus extends Enum[FlyStatus] {
  val values = findValues

  case object NORMAL extends FlyStatus()
  case object WARNING extends FlyStatus()
}

final case class StateOfFly(
    icao24: String,
    callsign: Option[String],
    originCountry: String,
    time: Long,
    latitude: Double,
    baroAltitudeSlice: Long,
    baroAltitude: Double,
    verticalRate: Double,
    status: FlyStatus
)

object StateOfFlyJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit object FlyStatusFormat extends RootJsonFormat[FlyStatus] {
    def write(flyStatus: FlyStatus) = JsString(flyStatus.entryName)

    def read(value: JsValue) = value match {
      case JsString(flyStatus) => FlyStatus.withName(flyStatus)
      case _                   => deserializationError("Color expected")
    }
  }
  implicit val stateOfFlyFormat: RootJsonFormat[StateOfFly] = jsonFormat9(StateOfFly.apply)
}
