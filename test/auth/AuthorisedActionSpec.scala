package auth

import cats.effect.IO
import com.gu.identity.auth.*
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.*
import play.api.mvc.*
import play.api.mvc.Results.*
import play.api.test.*
import play.api.test.Helpers.*

import scala.concurrent.ExecutionContext.Implicits.global

class AuthorisedActionSpec extends PlaySpec {

  "refine" should {

    "return 401 when there is no Authorization header" in {
      val authService = mock[OktaAuthService]
      val action = new AuthorisedAction(authService, Nil)
      val request = FakeRequest(GET, "/")
      val result = action(Ok)(request)
      status(result) shouldBe UNAUTHORIZED
      contentType(result) shouldBe Some("text/plain")
      contentAsString(result) shouldBe "Request has no Authorization header"
    }

    "return 403 when the token is valid but doesn't have the required scopes" in {
      val requiredScopes = List(new AccessScope {
        override def name: String = "requiredScope"
      })
      val authService = mock[OktaAuthService]
      when(authService.validateAccessToken(AccessToken("validToken"), requiredScopes))
        .thenReturn(IO.raiseError(OktaValidationException(MissingRequiredScope(requiredScopes))))
      val action = new AuthorisedAction(authService, requiredScopes)
      val request = FakeRequest(GET, "/").withHeaders("Authorization" -> "Bearer validToken")
      val result = action(Ok)(request)
      status(result) shouldBe FORBIDDEN
      contentType(result) shouldBe Some("text/plain")
      contentAsString(result) shouldBe "Token is missing required scope(s): requiredScope"
    }

    "return 200 when the token is valid and has the required scopes" in {
      val requiredScopes = List(new AccessScope {
        override def name: String = "requiredScope"
      })
      val authService = mock[OktaAuthService]
      when(authService.validateAccessToken(AccessToken("validToken"), requiredScopes))
        .thenReturn(
          IO.pure(DefaultAccessClaims(primaryEmailAddress = "a@b.com", identityId = "I43", username = None))
        )
      val action = new AuthorisedAction(authService, requiredScopes)
      val request = FakeRequest(GET, "/").withHeaders("Authorization" -> "Bearer validToken")
      val result = action(Ok)(request)
      status(result) shouldBe OK
    }
  }
}
