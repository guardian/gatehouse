package model

import com.okta.sdk.resource.model.User as OktaUser
import utils.StringHelper.nonNullNonEmpty

case class Name(title: Option[String], firstName: Option[String], lastName: Option[String])

object Name {

  def fromOktaUser(oktaUser: OktaUser): Name =
    Name(
      title = nonNullNonEmpty(oktaUser.getProfile.getHonorificPrefix),
      firstName = nonNullNonEmpty(oktaUser.getProfile.getFirstName),
      lastName = nonNullNonEmpty(oktaUser.getProfile.getLastName)
    )
}
