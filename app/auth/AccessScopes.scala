package auth

import com.gu.identity.auth.AccessScope

object AccessScopes {

  case object UserReadSelfSecure extends AccessScope {
    val name = "guardian.identity-api.user.read.self.secure"
  }
}
