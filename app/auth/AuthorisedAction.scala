package auth

import com.gu.identity.auth.*
import play.api.mvc.*
import play.api.mvc.Results.*

import scala.concurrent.{ExecutionContext, Future}

/** An action that requires a valid access token with the given required access scopes. If the token is valid, the
  * request is passed to the next action in the chain.
  *
  * If the request doesn't have an Authorization header or the bearer token in the header isn't a valid access token,
  * the action returns a 401 Unauthorized response. If the token is valid but doesn't have the required scopes, the
  * action returns a 403 Forbidden response.
  */
class AuthorisedAction(
    oktaAuthService: OktaAuthService,
    val parser: BodyParser[AnyContent],
    requiredScopes: List[AccessScope]
)(implicit
    val executionContext: ExecutionContext
) extends ActionBuilder[RequestWithClaims, AnyContent]
    with ActionRefiner[Request, RequestWithClaims] {

  protected def refine[A](request: Request[A]): Future[Either[Result, RequestWithClaims[A]]] = {
    Helpers.fetchBearerTokenFromAuthHeader(request.headers.get) match {
      case Left(_) => Future.successful(Left(Unauthorized("Request has no Authorization header")))
      case Right(token) =>
        oktaAuthService
          .validateAccessToken(AccessToken(token), requiredScopes)
          .redeem(
            {
              case OktaValidationException(err: ValidationError) =>
                Left(new Status(err.suggestedHttpResponseCode)(err.message))
              case err => Left(InternalServerError(err.getMessage))
            },
            claims => Right(RequestWithClaims(claims, request))
          )
          .unsafeToFuture()
    }
  }
}
