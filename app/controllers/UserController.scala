package controllers

import auth.AccessScopes.UserReadSelfSecure
import auth.AuthorisedAction
import com.gu.identity.auth.AccessScope
import play.api.*
import play.api.mvc.*

class UserController(
    val controllerComponents: ControllerComponents,
    authorisedAction: List[AccessScope] => AuthorisedAction
) extends BaseController {

  def me(): Action[AnyContent] = authorisedAction(List(UserReadSelfSecure))(NotImplemented("TODO: implement me"))
}
