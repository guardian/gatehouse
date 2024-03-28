package auth

import com.gu.identity.auth.{DefaultAccessClaims, DefaultIdentityClaims, OktaAuthenticatedUserInfo}
import play.api.mvc.{Request, WrappedRequest}

class RequestWithClaims[A](
    val userInfo: OktaAuthenticatedUserInfo[DefaultIdentityClaims, DefaultAccessClaims],
    request: Request[A]
) extends WrappedRequest[A](request)
