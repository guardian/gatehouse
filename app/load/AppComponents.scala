package load

import auth.AuthorisedAction
import com.gu.identity.auth.{OktaAudience, OktaAuthService, OktaIssuerUrl, OktaTokenValidationConfig}
import com.okta.sdk.client.AuthorizationMode.PRIVATE_KEY
import com.okta.sdk.client.Clients
import com.okta.sdk.resource.api.UserApi as OktaUserApi
import controllers.{HealthCheckController, UserController}
import logging.RequestLoggingFilter
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.db.slick.{DbName, SlickApi, SlickComponents}
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.filters.HttpFiltersComponents
import router.Routes
import services.{CompositeUserService, LegacyIdentityDbUserService, OktaUserService}
import slick.jdbc.JdbcProfile

import scala.jdk.CollectionConverters.*

class AppComponents(context: Context)
    extends BaseAppComponents(context)
    with HttpFiltersComponents
    with SlickComponents
    with AhcWSComponents {

  override def slick: SlickApi = slickApi

  override def oktaUserApi: OktaUserApi = {
    val apiClient = Clients
      .builder()
      .setOrgUrl(oktaOrgUrl)
      .setClientId(configuration.get[String]("oktaApi.clientId"))
      .setAuthorizationMode(PRIVATE_KEY)
      .setPrivateKey(configuration.get[String]("oktaApi.privateKey"))
      .setScopes(configuration.get[Seq[String]]("oktaApi.scopes").toSet.asJava)
      .build()
    new OktaUserApi(apiClient)
  }
}

abstract class BaseAppComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with AhcWSComponents {

  def slick: SlickApi

  def oktaUserApi: OktaUserApi

  override def httpFilters: Seq[EssentialFilter] = super.httpFilters :+ new RequestLoggingFilter(materializer)

  protected lazy val oktaOrgUrl = s"https://${configuration.get[String]("oktaApi.domain")}"

  private lazy val authService = OktaAuthService(
    OktaTokenValidationConfig(
      issuerUrl = OktaIssuerUrl(configuration.get[String]("idProvider.issuer")),
      audience = Some(OktaAudience(configuration.get[String]("idProvider.audience"))),
      clientId = None
    )
  )

  private lazy val legacyIdentityDbUserService = new LegacyIdentityDbUserService(
    slick.dbConfig[JdbcProfile](DbName("legacyIdentityDb"))
  )

  private lazy val oktaUserService = new OktaUserService(oktaUserApi, oktaOrgUrl, wsClient)

  private lazy val userService = new CompositeUserService(oktaUserService, legacyIdentityDbUserService)

  private lazy val authorisedAction = new AuthorisedAction(authService, playBodyParsers.defaultBodyParser, _)

  lazy val healthCheckController = new HealthCheckController(controllerComponents, userService)

  private lazy val userController = new UserController(controllerComponents, authorisedAction, userService)

  lazy val router: Routes = new Routes(httpErrorHandler, healthCheckController, userController)
}
