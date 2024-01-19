package load

import play.api.ApplicationLoader.Context
import play.api.{BuiltInComponentsFromContext, Configuration}
import play.filters.HttpFiltersComponents
import router.Routes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.services.ssm.model.GetParameterRequest

import scala.util.Using

class AppComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents {

  private val region = Region.EU_WEST_1

  private val stage = context.initialConfiguration.getOptional[String]("stage").getOrElse("DEV")

  private lazy val secretKey: String = {
    val request = GetParameterRequest.builder
      .name(s"/$stage/identity/gatehouse/playSecret")
      .withDecryption(true)
      .build()
    Using.resource(SsmClient.builder.region(region).build()) {
      _.getParameter(request).parameter.value
    }
  }

  override def configuration: Configuration =
    if (stage == "DEV")
      super.configuration
    else
      Configuration("play.http.secret.key" -> secretKey).withFallback(super.configuration)

  lazy val healthCheckController = new controllers.HealthCheckController(controllerComponents)
  lazy val router: Routes = new Routes(httpErrorHandler, healthCheckController)
}
