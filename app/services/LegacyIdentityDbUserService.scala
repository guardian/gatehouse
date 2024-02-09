package services

import model.{TransientUser, User}
import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile
import slick.lifted.TableQuery

import scala.concurrent.{ExecutionContext, Future}

class LegacyIdentityDbUserService(val dbConfig: DatabaseConfig[PostgresProfile])(implicit ec: ExecutionContext)
    extends UserService {

  import dbConfig.profile.api.*

  private val db = dbConfig.db

  class UserTable(tag: Tag) extends Table[User](tag, "users") {
    def identityId = column[String]("id")
    def brazeId = column[String]("braze_uuid")
    def * = (identityId, brazeId).mapTo[User]
  }

  private val users = TableQuery[UserTable]

  def fetchUserByIdentityId(id: String): Future[Option[User]] = {
    val action = users.filter(_.identityId === id).take(1).result
    val result = db.run(action)
    result.map(_.headOption)
  }

  override def healthCheck(): Future[Unit] = ???

  override def createUser(fields: TransientUser): Future[User] = ???

  override def createTestUser(fields: TransientUser): Future[User] = ???

  override def updateUser(user: User): Future[Unit] = ???

  override def deleteUser(user: User): Future[Unit] = ???

  override def deletePhoneNumber(user: User): Future[Unit] = ???
}
