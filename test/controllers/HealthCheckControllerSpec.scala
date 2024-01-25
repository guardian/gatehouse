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

    val path = "/healthcheck"

    "run health check from a new instance of controller" in {
      val controller = new HealthCheckController(stubControllerComponents())
      val healthCheck = controller.healthCheck().apply(FakeRequest(GET, path))
      status(healthCheck) mustBe OK
    }

    "run health check from the router" in {
      val request = FakeRequest(GET, path)
      val healthCheck = route(app, request).get
      status(healthCheck) mustBe OK
    }
  }
}
