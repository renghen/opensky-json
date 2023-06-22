package models.opensky

import spray.json._

final case class TopCountry(name: String, count: Int)

object TopCountryJsonProtocol
    extends akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
    with DefaultJsonProtocol {

  implicit val timeAndStatesFormat: RootJsonFormat[TopCountry] = jsonFormat2(TopCountry.apply)
}
