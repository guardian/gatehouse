package services

import com.okta.sdk.client.AuthorizationMode.PRIVATE_KEY
import com.okta.sdk.client.Clients
import com.okta.sdk.resource.api.UserApi
import model.User
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logging}
import services.FutureHelper.*

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*
import scala.util.Failure
import scala.util.chaining.*

class OktaUserService(config: Configuration, ws: WSClient)(implicit ctx: ExecutionContext)
    extends UserService
    with Logging {

  private lazy val orgUrl = s"https://${config.get[String]("oktaApi.domain")}"

  private lazy val userApi = {
    val client = Clients
      .builder()
      .setOrgUrl(orgUrl)
      .setClientId(config.get[String]("oktaApi.clientId"))
      .setAuthorizationMode(PRIVATE_KEY)
      .setPrivateKey(config.get[String]("oktaApi.privateKey"))
      .setScopes(config.get[Seq[String]]("oktaApi.scopes").toSet.asJava)
      .build()
    new UserApi(client)
  }

  def healthCheck(): Future[Unit] =
    ws
      .url(s"$orgUrl/.well-known/openid-configuration")
      .get()
      .map(_ => ())
      .tap(_.onComplete {
        case Failure(exception) => logger.error(s"Health check failed: ${exception.getMessage}")
        case _                  => ()
      })

  def fetchUserByIdentityId(id: String): Future[Option[User]] =
    tryAsync(
      userApi
        .listUsers(null, null, 1, null, s"""profile.legacyIdentityId eq "$id"""", null, null)
        .asScala
        .headOption
        .map(User.fromOktaUser)
    )
}
