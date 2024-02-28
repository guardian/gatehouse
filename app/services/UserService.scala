package services

import model.User

import scala.concurrent.Future

trait UserService extends Service {

  def fetchUserByIdentityId(id: String): Future[Option[User]]
}
