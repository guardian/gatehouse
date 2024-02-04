package load

import logging.RequestLoggingFilter
import play.api.ApplicationLoader.Context
import play.api.mvc.EssentialFilter
import play.api.{BuiltInComponentsFromContext, Configuration}
import play.filters.HttpFiltersComponents
import router.Routes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.services.ssm.model.GetParameterRequest

import scala.util.Using

class AppComponents(context: Context) extends BuiltInComponentsFromContext(context) with HttpFiltersComponents {

  override def httpFilters: Seq[EssentialFilter] = super.httpFilters :+ new RequestLoggingFilter(materializer)

  lazy val healthCheckController = new controllers.HealthCheckController(controllerComponents)
  lazy val router: Routes = new Routes(httpErrorHandler, healthCheckController)
}
