package controllers

import play.api.*
import play.api.mvc.*

class HealthCheckController(val controllerComponents: ControllerComponents) extends BaseController {

  def healthCheck(): Action[AnyContent] = Action(Ok("OK"))
}
