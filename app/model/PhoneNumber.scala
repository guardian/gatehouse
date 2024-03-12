package model

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.okta.sdk.resource.model.User as OktaUser
import utils.StringHelper.nonNullNonEmpty

import scala.util.Try

case class PhoneNumber(countryCode: Int, localNumber: String)

object PhoneNumber {

  private val phoneUtil = PhoneNumberUtil.getInstance

  def fromOktaUser(oktaUser: OktaUser): Option[PhoneNumber] =
    nonNullNonEmpty(oktaUser.getProfile.getPrimaryPhone).flatMap(phoneNumber =>
      fromPhoneNumberString(phoneNumber, nonNullNonEmpty(oktaUser.getProfile.getCountryCode))
    )

  def fromPhoneNumberString(s: String, countryCode: Option[String]): Option[PhoneNumber] =
    Try(phoneUtil.parse(s, countryCode.orNull)).toOption.flatMap(phoneNumber =>
      Some(PhoneNumber(phoneNumber.getCountryCode, phoneNumber.getNationalNumber.toString))
    )
}
