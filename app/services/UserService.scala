package services

import model.User

import scala.concurrent.Future

trait UserService extends UpstreamService {

  def healthCheck(): Future[Unit]

  def fetchUserByIdentityId(id: String): Future[Option[User]]
}
