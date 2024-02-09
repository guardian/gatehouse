package load

import auth.AuthorisedAction
import com.gu.identity.auth.{OktaAudience, OktaAuthService, OktaIssuerUrl, OktaTokenValidationConfig}
import controllers.{HealthCheckController, UserController}
import logging.RequestLoggingFilter
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.mvc.EssentialFilter
import play.filters.HttpFiltersComponents
import router.Routes

class AppComponents(context: Context) extends BuiltInComponentsFromContext(context) with HttpFiltersComponents {

  override def httpFilters: Seq[EssentialFilter] = super.httpFilters :+ new RequestLoggingFilter(materializer)

  private lazy val authService = OktaAuthService(
    OktaTokenValidationConfig(
      issuerUrl = OktaIssuerUrl(context.initialConfiguration.get[String]("idProvider.issuer")),
      audience = Some(OktaAudience(context.initialConfiguration.get[String]("idProvider.audience"))),
      clientId = None,
    )
  )

  private lazy val authorisedAction = new AuthorisedAction(authService, _)

  private lazy val healthCheckController = new HealthCheckController(controllerComponents)
  private lazy val userController = new UserController(controllerComponents, authorisedAction)

  lazy val router: Routes = new Routes(httpErrorHandler, healthCheckController, userController)
}
