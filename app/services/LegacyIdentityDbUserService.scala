package services

import model.Permission
import play.api.Logging
import play.api.libs.json.{JsArray, JsValue, Json, Reads}
import slick.basic.DatabaseConfig
import slick.dbio.DBIO
import slick.jdbc.{JdbcProfile, PostgresProfile}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure
import scala.util.chaining.*

trait DbManager {

  def dbConfig: DatabaseConfig[_]

  def execute[T](dbio: DBIO[T]): Future[T] = dbConfig.db.run(dbio)

  def p = dbConfig.profile
}

class LegacyIdentityDbUserService(dbManager: DbManager)(implicit ctx: ExecutionContext)
    extends Logging {

  import dbManager.p..dbConfig.pro dbProfile.api._

  def healthCheck(): Future[Unit] =
    dbManager
      .execute(sql"SELECT 1".as[Int])
      .map(_ => ())
      .tap(_.onComplete {
        case Failure(exception) => logger.error(s"Health check failed: ${exception.getMessage}")
        case _                  => ()
      })

  def fetchUserByIdentityId(id: String): Future[Option[LegacyUser]] =
    dbManager
      .execute(
        sql"SELECT id, okta_id, braze_uuid, jdoc->'publicFields'->>'username', jdoc->'consents' FROM users WHERE id = $id LIMIT 1"
          .as[(String, Option[String], Option[String], Option[String], String)]
      )
      .map(_.map { case (identityId, oktaId, brazeId, userName, permissionsJsonStr) =>
        LegacyUser(identityId, oktaId, brazeId, userName, toPermissions(permissionsJsonStr))
      }.headOption)

  private def toPermissions(jsonStr: String): Seq[Permission] = {
    def toPermission(json: JsValue) =
      Permission(id = (json \ "id").as[String], permitted = (json \ "consented").as[Boolean])
    (Json.parse(jsonStr) match {
      case JsArray(jsons) => jsons
      case _              => Nil
    }).map(toPermission).toList
  }
}

case class LegacyUser(
    identityId: String,
    oktaId: Option[String],
    brazeId: Option[String],
    userName: Option[String],
    permissions: Seq[Permission]
)
