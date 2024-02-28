package services

import model.Permission
import play.api.Logging
import play.api.libs.json.{JsArray, JsValue, Json, Reads}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure
import scala.util.chaining.*

class LegacyIdentityDbUserService(val dbConfig: DatabaseConfig[JdbcProfile])(implicit ctx: ExecutionContext)
    extends LegacyUserService
    with Logging {

  import dbConfig.profile.api.*

  private val db = dbConfig.db

  def healthCheck(): Future[Unit] =
    db.run(sql"SELECT 1".as[Int])
      .map(_ => ())
      .tap(_.onComplete {
        case Failure(exception) => logger.error(s"Health check failed: ${exception.getMessage}")
        case _                  => ()
      })

  def fetchByIdentityId(id: String): Future[Option[LegacyUser]] =
    db.run(
      sql"SELECT id, braze_uuid, jdoc->'publicFields'->'username', jdoc->'consents' FROM users WHERE id = $id LIMIT 1"
        .as[(String, Option[String], Option[String], String)]
    ).map(_.map { case (identityId, brazeId, userName, permissionsJsonStr) =>
      LegacyUser(identityId, brazeId, userName, toPermissions(permissionsJsonStr))
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
    brazeId: Option[String],
    userName: Option[String],
    permissions: Seq[Permission]
)
