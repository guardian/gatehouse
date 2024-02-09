package model

import play.api.libs.json.{Json, Writes}

case class User(identityId: String, brazeId: String)

object User {
  implicit val writes: Writes[User] = Json.writes[User]
}
