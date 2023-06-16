package model.opensky

import spray.json._

case class State(
    icao24: String,
    callsign: Option[String],
    originCountry: String,
    timePosition: Option[Long],
    lastContact: Long,
    longitude: Option[Double],
    latitude: Option[Double],
    baroAltitude: Option[Double],
    onGround: Boolean,
    velocity: Option[Double],
    trueTrack: Option[Double],
    verticalRate: Option[Double],
    sensors: Option[List[Long]],
    geoAltitude: Option[Double],
    squawk: Option[String],
    spi: Boolean,
    positionSource: Int
)

object StateJsonProtocol extends DefaultJsonProtocol {

  //given more time will use magnet pattern
  def optionalString(field: Option[String]) = field match {
    case None        => JsNull
    case Some(value) => JsString(value)
  }

  def optionalNumberFromInt(field: Option[Long]) = field match {
    case None        => JsNull
    case Some(value) => JsNumber(value)
  }

  def optionalNumberfromDouble(field: Option[Double]) = field match {
    case None        => JsNull
    case Some(value) => JsNumber(value)
  }

  def optionalNumberFromListLong(field: Option[List[Long]]) = field match {
    case None        => JsNull
    case Some(value) => JsArray(value.map(JsNumber(_)).toVector)
  }

  implicit object StateJsonFormat extends RootJsonFormat[State] {
    def write(state: State) =
      JsArray(
        JsString(state.icao24),
        optionalString(state.callsign),
        JsString(state.originCountry),
        optionalNumberFromInt(state.timePosition),
        JsNumber(state.lastContact),
        optionalNumberfromDouble(state.longitude),
        optionalNumberfromDouble(state.latitude),
        optionalNumberfromDouble(state.baroAltitude),
        JsBoolean(state.onGround),
        optionalNumberfromDouble(state.velocity),
        optionalNumberfromDouble(state.trueTrack),
        optionalNumberfromDouble(state.verticalRate),
        optionalNumberFromListLong(state.sensors),
        optionalNumberfromDouble(state.geoAltitude),
        optionalString(state.squawk),
        JsBoolean(state.spi),
        JsNumber(state.positionSource)
      )

    def optionStringJs(js: JsValue): Option[String] = {
      js match {
        case JsNull          => None
        case JsString(value) => Some(value)
        case _               => None
      }
    }

    //given more time will use magnet pattern
    def optionDoubleJs(js: JsValue): Option[Double] = {
      js match {
        case JsNull          => None
        case JsNumber(value) => Some(value.toDouble)
        case _               => None
      }
    }

    def optionLongJs(js: JsValue): Option[Long] = {
      js match {
        case JsNull          => None
        case JsNumber(value) => Some(value.toLong)
        case _               => None
      }
    }

    def optionVectorJs(js: JsValue): Option[List[Long]] = {
      js match {
        case JsNull => None
        case JsArray(value) =>
          Some(value.collect { case (js: JsNumber) => js.value.toLong }.toList)

        case _ => None
      }
    }

    def read(value: JsValue) = value match {
      case JsArray(
            Vector(
              JsString(icao24),
              callsign,
              JsString(originCountry),
              timePosition,
              JsNumber(lastContact),
              longitude,
              latitude,
              baroAltitude,
              JsBoolean(onGround),
              velocity,
              trueTrack,
              verticalRate,
              sensors,
              geoAltitude,
              squawk,
              JsBoolean(spi),
              JsNumber(positionSource)
            )
          ) =>
        State(
          icao24,
          optionStringJs(callsign),
          originCountry,
          optionLongJs(timePosition),
          lastContact.toLong,
          optionDoubleJs(longitude),
          optionDoubleJs(latitude),
          optionDoubleJs(baroAltitude),
          onGround,
          optionDoubleJs(velocity),
          optionDoubleJs(trueTrack),
          optionDoubleJs(verticalRate),
          optionVectorJs(sensors),
          optionDoubleJs(geoAltitude),
          optionStringJs(squawk),
          spi,
          positionSource.toInt
        )

      case _ => deserializationError("State expected")
    }
  }
}