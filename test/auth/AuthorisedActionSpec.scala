package auth

import cats.effect.IO
import com.gu.identity.auth.*
import org.apache.pekko.stream.testkit.NoMaterializer
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.*
import play.api.libs.json.Json
import play.api.mvc.*
import play.api.mvc.Results.*
import play.api.test.*
import play.api.test.Helpers.*

import scala.concurrent.ExecutionContext.Implicits.global

class AuthorisedActionSpec extends PlaySpec {

  "refine" should {

    val requiredScopes = List(new AccessScope {
      override def name: String = "requiredScope"
    })

    "return 401 when there is no Authorization header" in {
      val authService = mock[OktaAuthService]
      val bodyParser = mock[BodyParser[AnyContent]]
      val action = new AuthorisedAction(authService, bodyParser, requiredScopes)
      val request = FakeRequest(GET, "/")
      val result = action(Ok)(request)
      status(result) shouldBe UNAUTHORIZED
      contentType(result) shouldBe Some("text/plain")
      contentAsString(result) shouldBe "Request has no Authorization header"
    }

    "return 401 when the token is invalid" in {
      val authService = mock[OktaAuthService]
      when(authService.validateAccessToken(AccessToken("invalidToken"), requiredScopes))
        .thenReturn(IO.raiseError(OktaValidationException(InvalidOrExpiredToken)))
      val bodyParser = mock[BodyParser[AnyContent]]
      val action = new AuthorisedAction(authService, bodyParser, requiredScopes)
      val request = FakeRequest(GET, "/").withHeaders("Authorization" -> "Bearer invalidToken")
      val result = action(Ok)(request)
      status(result) shouldBe UNAUTHORIZED
      contentType(result) shouldBe Some("text/plain")
      contentAsString(result) shouldBe "Access token validation failed."
    }

    "return 403 when the token is valid but doesn't have the required scopes" in {
      val authService = mock[OktaAuthService]
      when(authService.validateAccessToken(AccessToken("validToken"), requiredScopes))
        .thenReturn(IO.raiseError(OktaValidationException(MissingRequiredScope(requiredScopes))))
      val bodyParser = mock[BodyParser[AnyContent]]
      val action = new AuthorisedAction(authService, bodyParser, requiredScopes)
      val request = FakeRequest(GET, "/").withHeaders("Authorization" -> "Bearer validToken")
      val result = action(Ok)(request)
      status(result) shouldBe FORBIDDEN
      contentType(result) shouldBe Some("text/plain")
      contentAsString(result) shouldBe "Access token validation failed."
    }

    "return 200 when the token is valid and has the required scopes" in {
      val userInfo = OktaAuthenticatedUserInfo[DefaultIdentityClaims, DefaultAccessClaims](
        localAccessTokenClaims = DefaultAccessClaims(
          oktaId = "someOktaId",
          primaryEmailAddress = "a@b.com",
          identityId = "I43",
          username = None
        ),
        serverSideUserInfo = None
      )
      val authService = mock[OktaAuthService]
      when(authService.validateAccessToken(AccessToken("validToken"), requiredScopes))
        .thenReturn(
          IO.pure(userInfo)
        )
      val bodyParser = mock[BodyParser[AnyContent]]
      val action = new AuthorisedAction(authService, bodyParser, requiredScopes)
      val request = FakeRequest(GET, "/").withHeaders("Authorization" -> "Bearer validToken")
      val result = action(Ok)(request)
      status(result) shouldBe OK
    }

    "parse the request body correctly" in {
      val requestBody = Json.obj("key" -> "value")
      val request = FakeRequest(POST, "/").withHeaders("Authorization" -> "Bearer validToken").withJsonBody(requestBody)
      val authService = mock[OktaAuthService]
      val bodyParser = stubBodyParser(AnyContentAsJson(requestBody))
      val action = new AuthorisedAction(authService, bodyParser, requiredScopes)
      await(action.parser(request).run()(NoMaterializer)) match {
        case Left(result)   => fail(s"Request body parsing failed: $result")
        case Right(content) => content mustBe AnyContentAsJson(Json.obj("key" -> "value"))
      }
    }
  }
}
