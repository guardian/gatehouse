package controllers

import com.okta.sdk.resource.api.UserApi as OktaUserApi
import load.BaseAppComponents
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.*
import org.scalatestplus.play.components.OneAppPerSuiteWithComponents
import play.api.db.slick.{DbName, SlickApi}
import play.api.test.*
import play.api.test.Helpers.*
import services.UserService
import slick.basic.{BasicProfile, DatabaseConfig}
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HealthCheckControllerSpec extends PlaySpec with OneAppPerSuiteWithComponents with MockitoSugar {

  override def components: BaseAppComponents = new BaseAppComponents(context) {

    override def slick: SlickApi = new SlickApi {
      override def dbConfigs[P <: BasicProfile](): Seq[(DbName, DatabaseConfig[P])] = ???

      override def dbConfig[P <: BasicProfile](name: DbName): DatabaseConfig[P] = {
//        val z = mock[P]
        val y = mock[DatabaseConfig[P]]
        when(y.profile).thenReturn(mock[JdbcProfile])
        y
      }
    }

    override def oktaUserApi: OktaUserApi = mock[OktaUserApi]
  }

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

    "run health check from application" in {
      val controller = components.healthCheckController
      val healthCheck = controller.healthCheck().apply(FakeRequest(GET, path))
//      status(healthCheck) mustBe OK
//      contentType(healthCheck) mustBe Some("text/html")
      contentAsString(healthCheck) mustBe "OK"
    }

    "run health check from router" in {
      val request = FakeRequest(GET, path)
      val healthCheck = route(app, request).get
      status(healthCheck) mustBe OK
      contentType(healthCheck) mustBe Some("text/html")
      contentAsString(healthCheck) must include("Readings")
    }
  }
}
