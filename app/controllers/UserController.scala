package controllers

import auth.AccessScopes.UserReadSelfSecure
import auth.AuthorisedAction
import com.gu.identity.auth.AccessScope
import play.api.*
import play.api.mvc.*
import services.UserService

import scala.concurrent.ExecutionContext

class UserController(
    val controllerComponents: ControllerComponents,
    authorisedAction: List[AccessScope] => AuthorisedAction,
    userService: UserService
)(implicit ex: ExecutionContext)
    extends BaseController {

  def me(): Action[AnyContent] = authorisedAction(List(UserReadSelfSecure)).async(request =>
    userService
      .fetchUserByIdentityId(request.claims.identityId)
      .map(_.map(user => Ok(user.toString)).getOrElse(NotFound))
  )
}
