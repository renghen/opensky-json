package model.opensky

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
