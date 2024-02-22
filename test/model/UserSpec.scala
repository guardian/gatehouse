package model

import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class UserSpec extends PlaySpec {

  "writes.me" should {
    "generate correct Json response for /user/me" in {
      val user = User(
        identityId = "123",
        brazeId = None,
        emailAddress = Some("test@user"),
        emailValidated = Some(true),
        userName = Some("test@user"),
        name = Name(title = Some("Mx"), firstName = Some("Test"), secondName = Some("User")),
        address = Address(
          line1 = Some("1"),
          line2 = Some("2"),
          line3 = Some("3"),
          line4 = None,
          postcode = Some("W1A 1AA"),
          country = Some("United Kingdom"),
        ),
        phoneNumber = Some(PhoneNumber(44, "12345")),
        registrationLocation = Some("online"),
        permissions = Seq(Permission("p1", true), Permission("p2", false), Permission("p3", true)),
      )
      Json.stringify(Json.toJson(user)(User.writes.me)) shouldBe
        """{
          |"identityId":"123",
          |"emailAddress":"test@user",
          |"emailValidated":true,
          |"username":"test@user",
          |"title":"Mx",
          |"firstName":"Test",
          |"secondName":"User",
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
}
