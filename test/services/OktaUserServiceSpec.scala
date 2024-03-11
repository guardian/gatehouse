package services

import com.okta.sdk.resource.api.UserApi
import com.okta.sdk.resource.client.ApiException
import com.okta.sdk.resource.model.{UserProfile, User as OktaUser}
import model.{Address, Name, User}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.WSClient

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.*
import scala.util.Failure

class OktaUserServiceSpec extends PlaySpec with MockitoSugar {

  "fetchUserByOktaId" should {

    "give a user when one exists" in {
      val additionalProperties = Map("legacyIdentityId" -> "87654321").asJava

      val profile = mock[UserProfile]
      when(profile.getEmail).thenReturn("test@example.com")
      when(profile.getAdditionalProperties).thenReturn(additionalProperties)

      val oktaUser = mock[OktaUser]
      when(oktaUser.getId).thenReturn("oktaId")
      when(oktaUser.getProfile).thenReturn(profile)

      val userApi = mock[UserApi]
      when(userApi.getUser("oktaId")).thenReturn(oktaUser)

      val oktaUserService = new OktaUserService(userApi, "orgUrl", mock[WSClient])

      val user = User(
        oktaId = "oktaId",
        legacyIdentityId = "87654321",
        emailAddress = Some("test@example.com"),
        emailValidated = None,
        userName = None,
        name = Name(None, None, None),
        address = Address(None, None, None, None, None, None),
        phoneNumber = None,
        registrationLocation = None,
        permissions = Nil
      )

      val result = oktaUserService.fetchUserByOktaId("oktaId")
      Await.result(result, Duration.Inf) mustBe Some(user)
    }

    "give no user when one doesn't exist" in {
      val exception = new ApiException(404, "Not found")

      val userApi = mock[UserApi]
      when(userApi.getUser("oktaId")).thenThrow(exception)

      val oktaUserService = new OktaUserService(userApi, "orgUrl", mock[WSClient])

      val result = oktaUserService.fetchUserByOktaId("oktaId")
      Await.result(result, Duration.Inf) mustBe None
    }

    "give a failed future when API fails" in {
      val exception = new ApiException(500, "Internal server error")

      val userApi = mock[UserApi]
      when(userApi.getUser("oktaId")).thenThrow(exception)

      val oktaUserService = new OktaUserService(userApi, "orgUrl", mock[WSClient])

      val result = oktaUserService.fetchUserByOktaId("oktaId")
      result.onComplete {
        case Failure(e) => e mustBe exception
        case _          => fail("Expected a failed future")
      }
    }
  }
}
