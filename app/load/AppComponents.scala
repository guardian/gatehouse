package load

import auth.AuthorisedAction
import com.gu.identity.auth.{OktaAudience, OktaAuthService, OktaIssuerUrl, OktaTokenValidationConfig}
import controllers.{HealthCheckController, UserController}
import logging.RequestLoggingFilter
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.db.slick.{DbName, SlickComponents}
import play.api.mvc.EssentialFilter
import play.filters.HttpFiltersComponents
import router.Routes
import services.LegacyIdentityDbUserService
import slick.jdbc.PostgresProfile

class AppComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with SlickComponents {

  override def httpFilters: Seq[EssentialFilter] = super.httpFilters :+ new RequestLoggingFilter(materializer)

  private lazy val authService = OktaAuthService(
    OktaTokenValidationConfig(
      issuerUrl = OktaIssuerUrl(context.initialConfiguration.get[String]("idProvider.issuer")),
      audience = Some(OktaAudience(context.initialConfiguration.get[String]("idProvider.audience"))),
      clientId = None,
    )
  )

  private lazy val authorisedAction = new AuthorisedAction(authService, _)

  private lazy val legacyIdentityDbUserService = new LegacyIdentityDbUserService(
    slickApi.dbConfig[PostgresProfile](DbName("legacyIdentityDb"))
  )

  private lazy val healthCheckController =
    new HealthCheckController(controllerComponents, Seq(legacyIdentityDbUserService))

  private lazy val userController =
    new UserController(controllerComponents, authorisedAction, legacyIdentityDbUserService)

  lazy val router: Routes = new Routes(httpErrorHandler, healthCheckController, userController)
}
