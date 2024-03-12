package services

import com.okta.sdk.resource.api.UserApi
import com.okta.sdk.resource.client.{ApiClient, ApiException}
import model.User
import play.api.Logging
import play.api.libs.ws.WSClient
import utils.FutureHelper.tryAsync

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure
import scala.util.chaining.scalaUtilChainingOps

class OktaUserService(userApi: UserApi, orgUrl: String, ws: WSClient)(implicit ctx: ExecutionContext) extends Logging {

  def healthCheck(): Future[Unit] =
    ws
      .url(s"$orgUrl/.well-known/openid-configuration")
      .get()
      .map(_ => ())
      .tap(_.onComplete {
        case Failure(exception) => logger.error(s"Health check failed: ${exception.getMessage}")
        case _                  => ()
      })

  def fetchUserByOktaId(oktaId: String): Future[Option[User]] =
    tryAsync(userApi.getUser(oktaId)).map(user => Some(User.fromOktaUser(user))).recover {
      case e: ApiException if e.getCode == 404 => None
    }
}
