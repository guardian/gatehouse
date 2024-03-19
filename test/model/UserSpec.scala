package model

import com.okta.sdk.resource.model.{UserProfile, User as OktaUser}
import org.mockito.Mockito.*
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

import scala.jdk.CollectionConverters.*

class UserSpec extends PlaySpec with MockitoSugar {

  "writes.me" should {
    "generate correct Json response for /user/me" in {
      val user = User(
        oktaId = "i321",
        legacyIdentityId = "123",
        emailAddress = Some("test@user"),
        emailValidated = Some(true),
        userName = Some("test@user"),
        name = Name(title = Some("Mx"), firstName = Some("Test"), lastName = Some("User")),
        address = Address(
          line1 = Some("1"),
          line2 = Some("2"),
          line3 = Some("3"),
          line4 = None,
          postcode = Some("W1A 1AA"),
          country = Some(Country.GB)
        ),
        phoneNumber = Some(PhoneNumber(44, "12345")),
        registrationLocation = Some("online"),
        permissions = Seq(Permission("p1", true), Permission("p2", false), Permission("p3", true))
      )
      Json.stringify(Json.toJson(user)(User.writes.me)) shouldBe
        """{
          |"identityId":"123",
          |"emailAddress":"test@user",
          |"emailValidated":true,
          |"username":"test@user",
          |"title":"Mx",
          |"firstName":"Test",
          |"lastName":"User",
          |"addressLine1":"1",
          |"addressLine2":"2",
          |"addressLine3":"3",
          |"addressLine4":null,
          |"postcode":"W1A 1AA",
          |"country":"United Kingdom",
          |"countryCode":44,
          |"localNumber":"12345",
          |"registrationLocation":"online",
          |"permissions":[{"id":"p1","permitted":true},{"id":"p2","permitted":false},{"id":"p3","permitted":true}]
          |}""".stripMargin.replace("\n", "")
    }
  }

  "fromOktaUser" should {

    "create a User from OktaUser when all fields are present and valid" in {
      val additionalProperties = Map("legacyIdentityId" -> "87654321").asJava
      val profile = mock[UserProfile]
      when(profile.getEmail).thenReturn("test@example.com")
      when(profile.getAdditionalProperties).thenReturn(additionalProperties)
      val oktaUser = mock[OktaUser]
      when(oktaUser.getId).thenReturn("12345678")
      when(oktaUser.getProfile).thenReturn(profile)
      val user = User.fromOktaUser(oktaUser)
      user.oktaId shouldBe "12345678"
      user.emailAddress shouldBe Some("test@example.com")
      user.legacyIdentityId shouldBe "87654321"
    }

    "create a User from OktaUser when some fields are missing" in {
      val additionalProperties = Map("legacyIdentityId" -> "87654321").asJava
      val profile = mock[UserProfile]
      when(profile.getEmail).thenReturn(null)
      when(profile.getAdditionalProperties).thenReturn(additionalProperties)
      val oktaUser = mock[OktaUser]
      when(oktaUser.getId).thenReturn("12345678")
      when(oktaUser.getProfile).thenReturn(profile)
      val user = User.fromOktaUser(oktaUser)
      user.oktaId shouldBe "12345678"
      user.emailAddress shouldBe None
      user.legacyIdentityId shouldBe "87654321"
    }

    "create a User from OktaUser when some fields are present but have invalid values" in {
      val additionalProperties = Map("legacyIdentityId" -> "87654321").asJava
      val profile = mock[UserProfile]
      when(profile.getEmail).thenReturn(" ")
      when(profile.getAdditionalProperties).thenReturn(additionalProperties)
      val oktaUser = mock[OktaUser]
      when(oktaUser.getId).thenReturn("12345678")
      when(oktaUser.getProfile).thenReturn(profile)
      val user = User.fromOktaUser(oktaUser)
      user.oktaId shouldBe "12345678"
      user.emailAddress shouldBe None
      user.legacyIdentityId shouldBe "87654321"
    }
  }
}
