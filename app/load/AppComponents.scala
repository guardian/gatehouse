package load

import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.filters.HttpFiltersComponents
import router.Routes

class AppComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents {
  lazy val healthCheckController = new controllers.HealthCheckController(controllerComponents)
  lazy val router: Routes = new Routes(httpErrorHandler, healthCheckController)
}
