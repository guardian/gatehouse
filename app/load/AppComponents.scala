package load

import auth.AuthorisedAction
import com.gu.identity.auth.{OktaAudience, OktaAuthService, OktaIssuerUrl, OktaTokenValidationConfig}
import com.okta.sdk.client.AuthorizationMode.PRIVATE_KEY
import com.okta.sdk.client.Clients
import com.okta.sdk.resource.api.UserApi
import controllers.{HealthCheckController, TelemetryFilter, UserController}
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.{ContextPropagators, TextMapPropagator}
import io.opentelemetry.contrib.aws.resource.Ec2Resource
import io.opentelemetry.contrib.awsxray.{AwsXrayIdGenerator, AwsXrayRemoteSampler}
import io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagator
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.BatchSpanProcessor
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

  private val openTelemetry = {
    val propagators = ContextPropagators.create(
      TextMapPropagator.composite(
        W3CTraceContextPropagator.getInstance,
        AwsXrayPropagator.getInstance
      )
    )
    val spanProcessor = BatchSpanProcessor.builder(OtlpGrpcSpanExporter.getDefault).build()
    val idGenerator = AwsXrayIdGenerator.getInstance
    val resource = Resource.getDefault.merge(Ec2Resource.get)
    val sampler = AwsXrayRemoteSampler.newBuilder(resource).build()
    val tracerProvider = SdkTracerProvider.builder
      .addSpanProcessor(spanProcessor)
      .setIdGenerator(idGenerator)
      .setResource(resource)
      .setSampler(sampler)
      .build()
    OpenTelemetrySdk.builder
      .setPropagators(propagators)
      .setTracerProvider(tracerProvider)
      .buildAndRegisterGlobal()
  }

  override def httpFilters: Seq[EssentialFilter] = super.httpFilters :++ Seq(
    new RequestLoggingFilter(materializer),
    new TelemetryFilter(openTelemetry.getTracer("Gatehouse-manual"))
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
