package controllers

import load.AppComponents
import org.scalatestplus.play.*
import org.scalatestplus.play.components.OneAppPerTestWithComponents
import play.api.BuiltInComponents
import play.api.test.*
import play.api.test.Helpers.*

class HealthCheckControllerSpec extends PlaySpec with OneAppPerTestWithComponents {

  override def components: BuiltInComponents = new AppComponents(context)

  "GET healthcheck" should {

    "run health check from a new instance of controller" in {
      val controller = new HealthCheckController(stubControllerComponents())
      val healthCheck = controller.healthCheck().apply(FakeRequest(GET, "/management/healthcheck"))
      status(healthCheck) mustBe NOT_IMPLEMENTED
      contentType(healthCheck) mustBe Some("text/html")
      contentAsString(healthCheck) must include("TODO")
    }

    "run health check from the router" in {
      val request = FakeRequest(GET, "/management/healthcheck")
      val healthCheck = route(app, request).get
      status(healthCheck) mustBe NOT_IMPLEMENTED
      contentType(healthCheck) mustBe Some("text/html")
      contentAsString(healthCheck) must include("TODO")
    }
  }
}
