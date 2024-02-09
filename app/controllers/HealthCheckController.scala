package controllers

import play.api.*
import play.api.mvc.*
import services.BackendService

import scala.concurrent.{ExecutionContext, Future}

class HealthCheckController(val controllerComponents: ControllerComponents, backendServices: Seq[BackendService])(
    implicit ec: ExecutionContext
) extends BaseController {

  def healthCheck(): Action[AnyContent] = Action.async(
    Future
      .sequence(backendServices.map(_.healthCheck()))
      .map { _ => Ok("OK") }
      .recover { case err => InternalServerError(err.getMessage) }
  )
}
