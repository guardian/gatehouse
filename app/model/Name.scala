package model

import com.okta.sdk.resource.model.User as OktaUser
import model.User.optionalProfileField

case class Name(title: Option[String], firstName: Option[String], lastName: Option[String])

object Name {

  def fromOktaUser(oktaUser: OktaUser): Name =
    Name(
      title = optionalProfileField(oktaUser.getProfile.getTitle),
      firstName = optionalProfileField(oktaUser.getProfile.getFirstName),
      lastName = optionalProfileField(oktaUser.getProfile.getLastName)
    )
}
