package model

import com.okta.sdk.resource.model.User as OktaUser
import play.api.libs.json.{Json, Writes}
import utils.StringHelper.nonNullNonEmpty

case class User(
    oktaId: String,
    legacyIdentityId: String,
    emailAddress: Option[String],
    emailValidated: Option[Boolean],
    userName: Option[String],
    name: Name,
    address: Address,
    phoneNumber: Option[PhoneNumber],
    registrationLocation: Option[String],
    permissions: Seq[Permission]
)

object User {

  def fromOktaUser(oktaUser: OktaUser): User = {
    User(
      oktaId = oktaUser.getId,
      legacyIdentityId = requiredCustomProfileField(oktaUser, "legacyIdentityId"),
      emailAddress = nonNullNonEmpty(oktaUser.getProfile.getEmail),
      emailValidated = customProfileField(oktaUser, "emailValidated").flatMap(_.toBooleanOption),
      userName = None,
      name = Name.fromOktaUser(oktaUser),
      address = Address.fromOktaUser(oktaUser),
      phoneNumber = PhoneNumber.fromOktaUser(oktaUser),
      registrationLocation = customProfileField(oktaUser, "registrationLocation"),
      permissions = Nil
    )
  }

  def customProfileField(oktaUser: OktaUser, fieldName: String): Option[String] =
    Option(oktaUser.getProfile.getAdditionalProperties.get(fieldName)).collect {
      case s: String if !s.isBlank => s
    }

  def requiredCustomProfileField(oktaUser: OktaUser, fieldName: String): String =
    oktaUser.getProfile.getAdditionalProperties.get(fieldName).asInstanceOf[String]

  object writes {

    // Not implicit because there will be multiple representations of a User and we want to be explicit about which one we're using
    val me: Writes[User] = user =>
      Json.obj(
        "identityId" -> user.legacyIdentityId,
        "emailAddress" -> user.emailAddress,
        "emailValidated" -> user.emailValidated,
        "username" -> user.userName,
        "title" -> user.name.title,
        "firstName" -> user.name.firstName,
        "lastName" -> user.name.lastName,
        "addressLine1" -> user.address.line1,
        "addressLine2" -> user.address.line2,
        "addressLine3" -> user.address.line3,
        "addressLine4" -> user.address.line4,
        "postcode" -> user.address.postcode,
        "country" -> user.address.country.map(_.name),
        "countryCode" -> user.phoneNumber.map(_.countryCode),
        "localNumber" -> user.phoneNumber.map(_.localNumber),
        "registrationLocation" -> user.registrationLocation,
        "permissions" -> user.permissions
      )
  }
}
