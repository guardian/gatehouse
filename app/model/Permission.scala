package model

import play.api.libs.json.{Json, Writes}

case class Permission(id: String, permitted: Boolean)

object Permission {
  implicit val writes: Writes[Permission] = Json.writes[Permission]
}
