package services

import model.User

import scala.concurrent.Future

trait UserService extends Service {

  def fetchUserByOktaId(oktaId: String): Future[Option[User]]
}
