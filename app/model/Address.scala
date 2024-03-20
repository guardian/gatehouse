package model

import com.okta.sdk.resource.model.User as OktaUser
import model.User.customProfileField
import utils.StringHelper.nonNullNonEmpty

case class Address(
    line1: Option[String],
    line2: Option[String],
    line3: Option[String],
    line4: Option[String],
    postcode: Option[String],
    country: Option[Country]
)

object Address {

  def fromOktaUser(oktaUser: OktaUser): Address =
    Address(
      line1 = customProfileField(oktaUser, "address1"),
      line2 = customProfileField(oktaUser, "address2"),
      line3 = customProfileField(oktaUser, "address3"),
      line4 = customProfileField(oktaUser, "address4"),
      postcode = nonNullNonEmpty(oktaUser.getProfile.getZipCode),
      country = Country.findByCode(oktaUser.getProfile.getCountryCode)
    )

}
