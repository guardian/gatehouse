package controllers

import play.api.*
import play.api.mvc.*

import javax.inject.*

class HealthCheckController(val controllerComponents: ControllerComponents) extends BaseController {

  def healthCheck(): Action[AnyContent] = TODO
}
