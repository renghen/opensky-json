package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import scala.concurrent.ExecutionContext
import models.opensky._
import scala.util.Success
import scala.concurrent.Future
import scala.util.Failure

/** This controller creates an `Action` to handle HTTP requests to the application's home page.
  */
@Singleton
class HomeController @Inject() (
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BaseController {

  /** Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method will be called when the application receives a `GET`
    * request with a path of `/`.
    */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def getPlanes() = Action.async { implicit request: Request[AnyContent] =>
    // fetchTimeAndState
    //   .getAirPlanes()
    //   .map { states =>
    //     Ok(states.map(_.icao24).length.toString())
    //   }
    Future.successful(1).map { result => Ok(result.toString()) }
  }

}
