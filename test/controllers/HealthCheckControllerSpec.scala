package controllers

import load.AppComponents
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.*
import org.scalatestplus.play.components.OneAppPerTestWithComponents
import play.api.BuiltInComponents
import play.api.test.*
import play.api.test.Helpers.*
import services.UserService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HealthCheckControllerSpec extends PlaySpec with OneAppPerTestWithComponents with MockitoSugar {

  override def components: BuiltInComponents = new AppComponents(context)

  "GET healthcheck" should {

    val path = "/healthcheck"

    "run health check from a new instance of controller" in {
      val userService = mock[UserService]
      when(userService.healthCheck()).thenReturn(Future.successful(()))
      val controller = new HealthCheckController(stubControllerComponents(), userService)
      val healthCheck = controller.healthCheck().apply(FakeRequest(GET, path))
      status(healthCheck) mustBe OK
      contentType(healthCheck) mustBe Some("text/plain")
      contentAsString(healthCheck) mustBe "OK"
    }
  }
}
