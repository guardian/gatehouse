package controllers

import auth.AccessScopes.UserReadSelfSecure
import auth.AuthorisedAction
import com.gu.identity.auth.AccessScope
import model.User
import model.User.writes
import play.api.*
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
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
      .map(_.map(user => Ok(toJson(user)(writes.me))).getOrElse(NotFound))
  )
}
