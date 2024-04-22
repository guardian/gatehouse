package load

import com.gu.conf.{
  ComposedConfigurationLocation,
  ConfigurationLoader,
  ResourceConfigurationLocation,
  SSMConfigurationLocation
}
import com.gu.{AppIdentity, AwsIdentity}
import com.typesafe.config.Config
import play.api.*
import play.api.ApplicationLoader.Context
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider

import scala.util.{Failure, Success, Try}

class AppLoader extends ApplicationLoader {

  private val appName = "gatehouse"
  private val awsProfileName = "identity"

  private def buildConfig(context: Context): Try[Config] = {
    val credentialsProvider = DefaultCredentialsProvider.builder.profileName(awsProfileName).build()
    val isDev = context.environment.mode == Mode.Dev

    def configFor(appIdentity: AppIdentity) = {
      ConfigurationLoader.load(appIdentity, credentialsProvider) { case identity: AwsIdentity =>
        ComposedConfigurationLocation(
          List(
            SSMConfigurationLocation.default(identity),
            ResourceConfigurationLocation(s"${identity.stage}.conf"),
          )
        )
      }
    }

    // To validate the SSL certificate of the RDS instance - remove this and the associated params when we no longer use RDS
    def configureTrustStore(config: Config) = {
      sys.Prop.StringProp("javax.net.ssl.trustStore").set(config.getString("trustStore.path"))
      sys.Prop.StringProp("javax.net.ssl.trustStorePassword").set(config.getString("trustStore.password"))
    }

    def configureTelemetry() = {
      sys.Prop.StringProp("otel.service.name").set("Gatehouse")
      sys.Prop.StringProp("otel.traces.exporter").set("logging,otlp")
      sys.Prop.StringProp("otel.metrics.exporter").set("none")
      sys.Prop.StringProp("otel.logs.exporter").set("none")
      sys.Prop.IntProp("otel.metric.export.interval").set("15000")
    }

    if (isDev)
      configureTelemetry()
      Try(configFor(AwsIdentity(app = appName, stack = awsProfileName, stage = "DEV", region = "eu-west-1")))
    else
      for {
        identity <- AppIdentity.whoAmI(defaultAppName = appName, credentialsProvider)
        config <- Try(configFor(identity))
        _ <- Try(configureTrustStore(config))
      } yield config
  }

  override def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader) foreach { _.configure(context.environment) }
    buildConfig(context) match {
      case Success(config) =>
        val newContext =
          context.copy(initialConfiguration = Configuration(config).withFallback(context.initialConfiguration))
        new AppComponents(newContext).application
      case Failure(exception) =>
        throw exception
    }
  }
}
