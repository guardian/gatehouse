package services

import model.{TransientUser, User}
import play.api.libs.json.Json
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import scala.concurrent.{ExecutionContext, Future}

class LegacyIdentityDbUserService(val dbConfig: DatabaseConfig[JdbcProfile])(implicit ec: ExecutionContext)
    extends UserService {

  import dbConfig.profile.api.*

  private val db = dbConfig.db

  private class UserTable(tag: Tag) extends Table[User](tag, "users") {
    def identityId = column[String]("id")
    def brazeId = column[String]("braze_uuid")
    def * = (identityId, brazeId).mapTo[User]
  }

  private val users = TableQuery[UserTable]

  override def healthCheck(): Future[Unit] =
    db.run(sql"SELECT 1".as[Int]).map(_ => ())

  def fetchUserByIdentityId(id: String): Future[Option[User]] = {
    val action = users.filter(_.identityId === id).take(1).result
    val result = db.run(action)
    result.map(_.headOption)
  }

  override def createUser(fields: TransientUser): Future[User] = ???

  override def createTestUser(fields: TransientUser): Future[User] = ???

  override def updateUser(user: User): Future[Unit] = ???

  override def deleteUser(user: User): Future[Unit] = ???

  override def deletePhoneNumber(user: User): Future[Unit] = ???
}
