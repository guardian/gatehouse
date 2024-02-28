package model

import com.okta.sdk.resource.model.User as OktaUser

case class PhoneNumber(countryCode: Int, localNumber: String)

object PhoneNumber {
  def fromOktaUser(oktaUser: OktaUser): Option[PhoneNumber] =
    Option(oktaUser.getProfile.getPrimaryPhone).flatMap(fromPhoneNumberString)

  private val phoneNumberPattern = "\\+(\\d{1,2}) (.+)".r

  def fromPhoneNumberString(s: String): Option[PhoneNumber] =
    s match {
      case phoneNumberPattern(countryCode, localNumber) => Some(PhoneNumber(countryCode.toInt, localNumber.trim))
      case _                                            => None
    }
}
