package services

import model.{TransientUser, User}

import scala.concurrent.Future

/** Backend service that manages users. */
trait UserService extends BackendService {

  def healthCheck(): Future[Unit]

  def fetchUserByIdentityId(id: String): Future[Option[User]]

  def createUser(fields: TransientUser): Future[User]

  def createTestUser(fields: TransientUser): Future[User]

  def updateUser(user: User): Future[Unit]

  def deleteUser(user: User): Future[Unit]

  def deletePhoneNumber(user: User): Future[Unit]
}
