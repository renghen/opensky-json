package controllers

import javax.inject._

import play.api._
import play.api.mvc._

import models.opensky._

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout

import spray.json._
import DefaultJsonProtocol._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.collection.immutable.ListMap

/** This controller is for the open sky api
  */
@Singleton
class OpenSkyFRPController @Inject() (
    val controllerComponents: ControllerComponents,
    val fetchTimeAndStateFRP: FetchTimeAndStateFRP
)(implicit ec: ExecutionContext)
    extends BaseController {
  val logger: Logger = Logger(this.getClass())
  implicit val timeout: Timeout = 5.seconds

  def top3Countries() = Action.async { implicit request: Request[AnyContent] =>
    import TopCountryJsonProtocol._
    val topCountriesFuture = fetchTimeAndStateFRP.topCountries()

    topCountriesFuture.map { topCountries =>
      if (topCountries.size == 0) {
        Ok("[]")
      } else {
        val top3 = topCountries.toSeq.sortBy(_._2)(Ordering.Int.reverse).take(3).map(TopCountry.tupled)
        val top3Json = top3.toJson.toString()
        logger.info(s"Sending... Top 3 countries: $top3Json")
        Ok(top3Json)
      }
    }
  }

  def overNetherlandsforlastHour() = Action.async { implicit request: Request[AnyContent] =>
    val overNetherlandsFuture = fetchTimeAndStateFRP.overNetherlands()

    overNetherlandsFuture.map { count =>
      val map = Map(("count" -> count))
      val countJson = map.toJson.toString()
      logger.info(s"sending... Planes over Netherlands within lastHour: $countJson")
      Ok(countJson)
    }
  }

  implicit object FlyStatusOrdering extends Ordering[FlyStatus] {
    def compare(x: FlyStatus, y: FlyStatus): Int =
      (x, y) match {
        // assuming that the ordering is Pending < InProgress < Success < Fail...
        case (_, _) if (x eq y)                    => 0
        case (FlyStatus.NORMAL, FlyStatus.WARNING) => 1
        case (FlyStatus.WARNING, FlyStatus.NORMAL) => -1
        case _                                     => 0
      }
  }

  def slice(id: Int) = Action.async { implicit request: Request[AnyContent] =>
    import StateOfFlyJsonProtocol._
    val slicesFuture = fetchTimeAndStateFRP.getSlices()

    slicesFuture.map { slices =>
      logger.info(s"slices size: ${slices.size}")
      slices.get(id) match {
        case None => Ok("[]")
        case Some(lst) => {
          logger.info(s"sending... planes in slice($id): ${lst.length}")
          val lstJson = lst.sortBy(_.status).toJson.toString()
          Ok(lstJson)
        }
      }
    }
  }
}
