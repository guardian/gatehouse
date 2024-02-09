package model

import play.api.libs.json.{Json, Writes}

case class User(
    identityId: String,
    brazeId: Option[String],
    emailAddress: Option[String],
    emailValidated: Option[Boolean],
    userName: Option[String],
    name: Name,
    address: Address,
    phoneNumber: Option[PhoneNumber],
    registrationLocation: Option[String],
    permissions: Seq[Permission],
)

object User {
  object writes {

    // Not implicit because there will be multiple representations of a User and we want to be explicit about which one we're using
    val me: Writes[User] = user =>
      Json.obj(
        "identityId" -> user.identityId,
        "emailAddress" -> user.emailAddress,
        "emailValidated" -> user.emailValidated,
        "username" -> user.userName,
        "title" -> user.name.title,
        "firstName" -> user.name.firstName,
        "secondName" -> user.name.secondName,
        "addressLine1" -> user.address.line1,
        "addressLine2" -> user.address.line2,
        "addressLine3" -> user.address.line3,
        "addressLine4" -> user.address.line4,
        "postcode" -> user.address.postcode,
        "country" -> user.address.country,
        "countryCode" -> user.phoneNumber.map(_.countryCode),
        "localNumber" -> user.phoneNumber.map(_.localNumber),
        "registrationLocation" -> user.registrationLocation,
        "permissions" -> user.permissions,
      )
  }
}
