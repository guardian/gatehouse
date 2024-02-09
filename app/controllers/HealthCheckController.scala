package controllers

import play.api.*
import play.api.mvc.*
import services.UpstreamService

import scala.concurrent.{ExecutionContext, Future}

class HealthCheckController(val controllerComponents: ControllerComponents, upstreamServices: Seq[UpstreamService])(
    implicit ec: ExecutionContext
) extends BaseController {

  def healthCheck(): Action[AnyContent] = Action.async(
    Future
      .sequence(upstreamServices.map(_.healthCheck()))
      .map { _ => Ok("OK") }
      .recover { case err => InternalServerError(err.getMessage) }
  )
}
