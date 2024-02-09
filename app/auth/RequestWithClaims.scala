package auth

import com.gu.identity.auth.DefaultAccessClaims
import play.api.mvc.{Request, WrappedRequest}

class RequestWithClaims[A](val claims: DefaultAccessClaims, request: Request[A]) extends WrappedRequest[A](request)
