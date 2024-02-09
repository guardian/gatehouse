package services

import model.{Address, Name, Permission, PhoneNumber, User}
import play.api.libs.json.{JsValue, Json, Reads}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import scala.concurrent.{ExecutionContext, Future}

class LegacyIdentityDbUserService(val dbConfig: DatabaseConfig[JdbcProfile])(implicit ec: ExecutionContext)
    extends UserService {

  import dbConfig.profile.api.*

  private val db = dbConfig.db

  private class UserTable(tag: Tag) extends Table[UserRow](tag, "users") {
    def identityId = column[String]("id")
    def brazeId = column[Option[String]]("braze_uuid")
    def jdoc = column[String]("jdoc")

    def * = (identityId, brazeId, jdoc).mapTo[UserRow]
  }

  private val users = TableQuery[UserTable]

  override def healthCheck(): Future[Unit] =
    db.run(sql"SELECT 1".as[Int]).map(_ => ())

  def fetchUserByIdentityId(id: String): Future[Option[User]] = {
    val action = users.filter(_.identityId === id).take(1).result
    val result = db.run(action)
    result.map(_.map(UserRow.toUser).headOption)
  }
}

case class UserRow(identityId: String, brazeId: Option[String], jdoc: String)

object UserRow {

  def toUser(row: UserRow): User = {
    // Would be more efficient to use Postgres native Json support here - will see how much better it is in practice
    val jdoc = Json.parse(row.jdoc)
    val phoneNumber = for {
      countryCode <- (jdoc \ "privateFields" \ "telephoneNumber" \ "countryCode").asOpt[String].flatMap(_.toIntOption)
      localNumber <- (jdoc \ "privateFields" \ "telephoneNumber" \ "localNumber").asOpt[String]
    } yield {
      PhoneNumber(countryCode, localNumber)
    }
    User(
      identityId = row.identityId,
      brazeId = row.brazeId,
      userName = (jdoc \ "publicFields" \ "username").asOpt[String].filterNot(_.isBlank),
      emailAddress = (jdoc \ "primaryEmailAddress").asOpt[String].filterNot(_.isBlank),
      emailValidated = (jdoc \ "statusFields" \ "userEmailValidated").asOpt[Boolean],
      name = Name(
        title = (jdoc \ "privateFields" \ "title").asOpt[String].filterNot(_.isBlank),
        firstName = (jdoc \ "privateFields" \ "firstName").asOpt[String].filterNot(_.isBlank),
        secondName = (jdoc \ "privateFields" \ "secondName").asOpt[String].filterNot(_.isBlank)
      ),
      address = Address(
        line1 = (jdoc \ "privateFields" \ "address1").asOpt[String].filterNot(_.isBlank),
        line2 = (jdoc \ "privateFields" \ "address2").asOpt[String].filterNot(_.isBlank),
        line3 = (jdoc \ "privateFields" \ "address3").asOpt[String].filterNot(_.isBlank),
        line4 = (jdoc \ "privateFields" \ "address4").asOpt[String].filterNot(_.isBlank),
        postcode = (jdoc \ "privateFields" \ "postcode").asOpt[String].filterNot(_.isBlank),
        country = (jdoc \ "privateFields" \ "country").asOpt[String].filterNot(_.isBlank),
      ),
      phoneNumber = phoneNumber,
      registrationLocation = (jdoc \ "privateFields" \ "registrationLocation").asOpt[String].filterNot(_.isBlank),
      permissions = (jdoc \ "consents")
        .as[Seq[JsValue]]
        .map(p => Permission(id = (p \ "id").as[String], permitted = (p \ "consented").as[Boolean]))
        .toList,
    )
  }
}
