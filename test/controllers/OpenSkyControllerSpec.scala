package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import models.opensky._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import controllers.OpenSkyFRPController

/** Add your spec here. You can mock out a whole application including requests, plugins etc.
  *
  * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
  */
class OpenSkyControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  "OpenSkyController GET" should {

    "top3Countries controller" in {
      // TODO :to implement
      assert(true)
    }

    // "render the index page from a new instance of controller" in {
    //   val controller = new OpenSkyController(stubControllerComponents())
    //   val home = controller.index().apply(FakeRequest(GET, "/"))

    //   status(home) mustBe OK
    //   contentType(home) mustBe Some("text/html")
    //   contentAsString(home) must include("Welcome to Play")
    // }

    //   "render the index page from the application" in {
    //     val controller = inject[OpenSkyController]
    //     val home = controller.index().apply(FakeRequest(GET, "/"))

    //     status(home) mustBe OK
    //     contentType(home) mustBe Some("text/html")
    //     contentAsString(home) must include("Welcome to Play")
    //   }

    //   "render the index page from the router" in {
    //     val request = FakeRequest(GET, "/")
    //     val home = route(app, request).get

    //     status(home) mustBe OK
    //     contentType(home) mustBe Some("text/html")
    //     contentAsString(home) must include("Welcome to Play")
    //   }
  }
}
