package model.opensky

import org.scalatest._
import org.scalatestplus.play.PlaySpec
import matchers._
import play.api.test.Helpers._

import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import org.scalatest.concurrent.ScalaFutures

class StateProcessingTest(implicit ec: ExecutionContext) extends StateProcessing {
  final val delay: Int = 2
}

class StateProcessingSpec extends PlaySpec with ScalaFutures {

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  val stateProcessing = new StateProcessingTest()

  "State Processing set up" must {
    "delay is 2 seconds" in {
      assert(stateProcessing.delay == 2)
    }
  }
}
