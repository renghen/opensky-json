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
class OpenSkyController @Inject() (
    val controllerComponents: ControllerComponents,
    @Named("fetchTimeAndState-actor") fetchTimeAndStateActor: ActorRef
)(implicit ec: ExecutionContext)
    extends BaseController {
  val logger: Logger = Logger(this.getClass())
  implicit val timeout: Timeout = 5.seconds
  import FetchTimeAndStateActor._

  def top3Countries() = Action.async { implicit request: Request[AnyContent] =>
    val topCountriesFuture = (fetchTimeAndStateActor ? TopCountries).mapTo[Map[String, Int]]
    topCountriesFuture.map { topCountries =>
      if (topCountries.size == 0) {
        Ok("{}")
      } else {
        val top3 = ListMap(topCountries.toSeq.sortWith(_._2 > _._2): _*).take(3).toMap
        val top3Json = top3.toJson.toString()
        logger.info(s"Top 3 countries: $top3Json")
        Ok(top3Json)
      }
    }
  }

  def overNetherlandsforlastHour() = Action.async { implicit request: Request[AnyContent] =>
    val overNetherlandsFuture = (fetchTimeAndStateActor ? OverNetherlands).mapTo[Int]
    overNetherlandsFuture.map { count =>
      val map = Map(("count" -> count))
      val countJson = map.toJson.toString()
      logger.info(s"Planes over Netherlands within lastHour: $count")
      Ok(countJson)
    }
  }

  def slice(id: Int) = Action.async { implicit request: Request[AnyContent] =>
    import StateOfFlyJsonProtocol._
    val slicesFuture = (fetchTimeAndStateActor ? GetSlices).mapTo[Map[Long, List[StateOfFly]]]

    slicesFuture.map { slices =>
      logger.info(s"slices size: ${slices.size}")
      slices.get(id) match {
        case None => Ok("[]")
        case Some(lst) => {
          logger.info(s"planes in slice($id): $lst")
          val lstJson = lst.toJson.toString()
          Ok(lstJson)
        }
      }
    }
  }
}
