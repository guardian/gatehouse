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

  private def buildConfig(context: Context): Try[Config] = {
    val credentialsProvider = DefaultCredentialsProvider.create()
    val isDev = context.environment.mode == Mode.Dev
    for
      identity <-
        if isDev then Success(AwsIdentity(app = appName, stack = "identity", stage = "DEV", region = "eu-west-1"))
        else AppIdentity.whoAmI(defaultAppName = appName, credentialsProvider)
      config <- Try(ConfigurationLoader.load(identity, credentialsProvider) { case identity: AwsIdentity =>
        ComposedConfigurationLocation(
          List(
            SSMConfigurationLocation.default(identity),
            ResourceConfigurationLocation(s"${identity.stage}.conf"),
          )
        )
      })
    yield config
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
