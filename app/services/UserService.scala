package services

import model.User

import scala.concurrent.Future

trait UserService extends Service {

  def fetchUserByIdentityId(identityId: String): Future[Option[User]]
}
