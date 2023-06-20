package model.opensky

final case class StateOfFly(
    icao24: String,
    callsign: Option[String],
    timePosition: Option[Long],
    lastContact: Long,
    latitude: Double,
    baroAltitudeSlice: Long,
    verticalRate: Double
)
