package load

import auth.AuthorisedAction
import com.gu.identity.auth.{OktaAudience, OktaAuthService, OktaIssuerUrl, OktaTokenValidationConfig}
import com.okta.sdk.client.AuthorizationMode.PRIVATE_KEY
import com.okta.sdk.client.Clients
import com.okta.sdk.resource.api.UserApi
import controllers.{HealthCheckController, TelemetryFilter, UserController}
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
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

import scala.jdk.CollectionConverters.*

class AppComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with SlickComponents
    with AhcWSComponents {

  private val openTelemetry = AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk

//  val openTelemetry: OpenTelemetry = {
//    val resource: Resource = Resource
//      .getDefault()
//      .toBuilder()
//      .put(ResourceAttributes.SERVICE_NAME, "Gatehouse")
//      .put(ResourceAttributes.SERVICE_VERSION, "0.1.0")
//      .build()
//
//    val sdkTracerProvider: SdkTracerProvider = SdkTracerProvider
//      .builder()
//      .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
//      .setResource(resource)
//      .build()
//
//    val sdkMeterProvider: SdkMeterProvider = SdkMeterProvider
//      .builder()
//      .registerMetricReader(PeriodicMetricReader.builder(LoggingMetricExporter.create()).build())
//      .setResource(resource)
//      .build()
//
//    val sdkLoggerProvider: SdkLoggerProvider = SdkLoggerProvider
//      .builder()
//      .addLogRecordProcessor(BatchLogRecordProcessor.builder(SystemOutLogRecordExporter.create()).build())
//      .setResource(resource)
//      .build();
//
//    val openTel: OpenTelemetry = OpenTelemetrySdk
//      .builder()
//      .setTracerProvider(sdkTracerProvider)
//      .setMeterProvider(sdkMeterProvider)
//      .setLoggerProvider(sdkLoggerProvider)
//      .setPropagators(
//        ContextPropagators.create(
//          TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(), W3CBaggagePropagator.getInstance())
//        )
//      )
//      .buildAndRegisterGlobal();
//
//    openTel
//  }

  override def httpFilters: Seq[EssentialFilter] = super.httpFilters :++ Seq(
    new RequestLoggingFilter(materializer),
    new TelemetryFilter(openTelemetry.getTracerProvider)
  )

  private lazy val oktaOrgUrl = s"https://${configuration.get[String]("oktaApi.domain")}"

  private lazy val oktaUserApi = {
    val apiClient = Clients
      .builder()
      .setOrgUrl(oktaOrgUrl)
      .setClientId(configuration.get[String]("oktaApi.clientId"))
      .setAuthorizationMode(PRIVATE_KEY)
      .setPrivateKey(configuration.get[String]("oktaApi.privateKey"))
      .setScopes(configuration.get[Seq[String]]("oktaApi.scopes").toSet.asJava)
      .build()
    new UserApi(apiClient)
  }

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

  private lazy val oktaUserService = new OktaUserService(oktaUserApi, oktaOrgUrl, wsClient)

  private lazy val userService = new CompositeUserService(oktaUserService, legacyIdentityDbUserService)

  private lazy val authorisedAction = new AuthorisedAction(authService, playBodyParsers.defaultBodyParser, _)

  private lazy val healthCheckController = new HealthCheckController(controllerComponents, userService)

  private lazy val userController = new UserController(controllerComponents, authorisedAction, userService)

  lazy val router: Routes = new Routes(httpErrorHandler, healthCheckController, userController)
}
