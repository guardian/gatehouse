package model

import com.okta.sdk.resource.model.{UserProfile, User as OktaUser}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

import scala.jdk.CollectionConverters.*

class AddressSpec extends PlaySpec with MockitoSugar {

  "fromOktaUser method" should {

    "correctly convert OktaUser with all fields filled" in {
      val additionalProperties = Map(
        "address1" -> "line1",
        "address2" -> "line2",
        "address3" -> "line3",
        "address4" -> "line4"
      ).asJava

      val profile = mock[UserProfile]
      when(profile.getZipCode).thenReturn("postcode")
      when(profile.getCountryCode).thenReturn("US")
      when(profile.getAdditionalProperties).thenReturn(additionalProperties)

      val oktaUser = mock[OktaUser]
      when(oktaUser.getProfile).thenReturn(profile)
      oktaUser.setProfile(profile)

      val address = Address.fromOktaUser(oktaUser)
      address mustBe Address(
        Some("line1"),
        Some("line2"),
        Some("line3"),
        Some("line4"),
        Some("postcode"),
        Some(Country.US)
      )
    }

    "correctly convert OktaUser with some fields filled" in {
      val additionalProperties = Map(
        "address1" -> "line1",
        "address2" -> "",
        "address3" -> "line3",
        "address4" -> ""
      ).asJava

      val profile = mock[UserProfile]
      when(profile.getZipCode).thenReturn("")
      when(profile.getCountryCode).thenReturn("US")
      when(profile.getAdditionalProperties).thenReturn(additionalProperties)

      val oktaUser = mock[OktaUser]
      when(oktaUser.getProfile).thenReturn(profile)
      oktaUser.setProfile(profile)

      val address = Address.fromOktaUser(oktaUser)
      address mustBe Address(
        Some("line1"),
        None,
        Some("line3"),
        None,
        None,
        Some(Country.US)
      )
    }

    "correctly convert OktaUser with no fields filled" in {
      val additionalProperties = Map(
        "address1" -> "",
        "address2" -> "",
        "address3" -> "",
        "address4" -> ""
      ).asJava

      val profile = mock[UserProfile]
      when(profile.getZipCode).thenReturn("")
      when(profile.getCountryCode).thenReturn("")
      when(profile.getAdditionalProperties).thenReturn(additionalProperties)

      val oktaUser = mock[OktaUser]
      when(oktaUser.getProfile).thenReturn(profile)
      oktaUser.setProfile(profile)

      val address = Address.fromOktaUser(oktaUser)
      address mustBe Address(
        None,
        None,
        None,
        None,
        None,
        None
      )
    }

    "correctly convert OktaUser with valid country code" in {
      val additionalProperties = Map(
        "address1" -> "line1",
        "address2" -> "line2",
        "address3" -> "line3",
        "address4" -> "line4"
      ).asJava

      val profile = mock[UserProfile]
      when(profile.getZipCode).thenReturn("postcode")
      when(profile.getCountryCode).thenReturn("US")
      when(profile.getAdditionalProperties).thenReturn(additionalProperties)

      val oktaUser = mock[OktaUser]
      when(oktaUser.getProfile).thenReturn(profile)
      oktaUser.setProfile(profile)

      val address = Address.fromOktaUser(oktaUser)
      address mustBe Address(
        Some("line1"),
        Some("line2"),
        Some("line3"),
        Some("line4"),
        Some("postcode"),
        Some(Country.US)
      )
    }
  }
}
