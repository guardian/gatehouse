package controllers

import load.AppComponents
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.play.*
import org.scalatestplus.play.components.OneAppPerTestWithComponents
import play.api.BuiltInComponents
import play.api.test.*
import play.api.test.Helpers.*

import scala.concurrent.ExecutionContext.Implicits.global

class HealthCheckControllerSpec extends PlaySpec with OneAppPerTestWithComponents {

  override def components: BuiltInComponents = new AppComponents(context)

  "GET healthcheck" should {

    val path = "/healthcheck"

    "run health check from a new instance of controller" in {
      val controller = new HealthCheckController(stubControllerComponents(), upstreamServices = Nil)
      val healthCheck = controller.healthCheck().apply(FakeRequest(GET, path))
      status(healthCheck) shouldBe OK
      contentType(healthCheck) shouldBe Some("text/plain")
      contentAsString(healthCheck) shouldBe "OK"
    }

    "run health check from the router" in {
      val request = FakeRequest(GET, path)
      val healthCheck = route(app, request).get
      status(healthCheck) shouldBe OK
      contentType(healthCheck) shouldBe Some("text/plain")
      contentAsString(healthCheck) shouldBe "OK"
    }
  }
}
