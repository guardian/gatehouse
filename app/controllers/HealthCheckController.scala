package controllers

import play.api.*
import play.api.mvc.*
import services.UserService

import scala.concurrent.ExecutionContext

class HealthCheckController(val controllerComponents: ControllerComponents, userService: UserService)(implicit
    ec: ExecutionContext
) extends BaseController {

  def healthCheck(): Action[AnyContent] = Action.async(
    userService
      .healthCheck()
      .map { _ => Ok("OK") }
      .recover { case err => InternalServerError(err.getMessage) }
  )
}
