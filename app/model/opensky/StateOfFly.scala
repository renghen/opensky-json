package model.opensky

final case class StateOfFly(
    icao24: String,
    callsign: Option[String],
    time: Long,
    latitude: Double,
    baroAltitudeSlice: Long,
    baroAltitude: Double,
    verticalRate: Double
)
