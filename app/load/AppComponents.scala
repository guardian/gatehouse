package load

import auth.AuthorisedAction
import com.gu.identity.auth.{OktaAudience, OktaAuthService, OktaIssuerUrl, OktaTokenValidationConfig}
import controllers.{HealthCheckController, UserController}
import logging.RequestLoggingFilter
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.db.slick.{DbName, SlickComponents}
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.filters.HttpFiltersComponents
import router.Routes
import services.{CompositeUserService, LegacyIdentityDbUserService, OktaUserService}
import slick.jdbc.JdbcProfile

class AppComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with SlickComponents
    with AhcWSComponents {

  override def httpFilters: Seq[EssentialFilter] = super.httpFilters :+ new RequestLoggingFilter(materializer)

  private lazy val authService = OktaAuthService(
    OktaTokenValidationConfig(
      issuerUrl = OktaIssuerUrl(configuration.get[String]("idProvider.issuer")),
      audience = Some(OktaAudience(configuration.get[String]("idProvider.audience"))),
      clientId = None
    )
  )

  private lazy val legacyIdentityDbUserService = new LegacyIdentityDbUserService(
    slickApi.dbConfig[JdbcProfile](DbName("legacyIdentityDb"))
  )

  private lazy val oktaUserService = new OktaUserService(configuration, wsClient)

  private lazy val userService = new CompositeUserService(oktaUserService, legacyIdentityDbUserService)

  private lazy val authorisedAction = new AuthorisedAction(authService, _)

  private lazy val healthCheckController = new HealthCheckController(controllerComponents, userService)

  private lazy val userController = new UserController(controllerComponents, authorisedAction, userService)

  lazy val router: Routes = new Routes(httpErrorHandler, healthCheckController, userController)
}
