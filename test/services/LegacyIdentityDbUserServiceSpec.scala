package services

import model.{Address, Name, Permission, PhoneNumber, User}
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.play.PlaySpec

class LegacyIdentityDbUserServiceSpec extends PlaySpec {

  "UserRow.toUser" should {
    "convert a data instance to a User correctly" in {
      val jdoc =
        """{
          |"id": "A1",
          |"dates": {"lastActivityDate": "2023-09-15T12:56:40Z", "accountCreatedDate": "2023-07-03T16:03:31Z"},
          |"consents": [{"id": "sms", "actor": "user", "version": 0, "consented": false, "timestamp": "2023-09-04T14:13:10Z", "privacyPolicyVersion": 1}, {"id": "digital_subscriber_preview", "actor": "user", "version": 0, "consented": true, "timestamp": "2023-08-04T14:13:10Z", "privacyPolicyVersion": 1}],
          |"userGroups": [{"path": "/sys/policies/basic-identity", "joinedDate": "2012-08-03T16:03:31Z", "packageCode": "CRW"}],
          |"socialLinks": [{"network": "google", "socialId": "123", "createdDate": "2023-07-15T13:21:44Z"}],
          |"publicFields": {"username": "abcdef", "vanityUrl": "abcdef"},
          |"searchFields": {"postcode": "n1 9gu", "username": "abcdef", "emailAddress": "a@b.com", "postcodePrefix": "n1"},
          |"statusFields": {"userEmailValidated": true, "allowThirdPartyProfiling": true},
          |"privateFields": {"title": "", "country": "India", "address1": "Guardian News and Media", "address2": "90 York Way", "address3": "London", "address4": "", "postcode": "N1 9GU", "brazeUuid": "B2", "firstName": "Testing", "puzzleUuid": "Puz53", "secondName": "It", "billingCountry": "", "registrationIp": "1.2.3.4", "billingAddress1": "", "billingAddress2": "", "billingAddress3": "", "billingAddress4": "", "billingPostcode": "", "telephoneNumber": {"countryCode": "91", "localNumber": "12345"}, "registrationLocation": "wherever"},
          |"primaryEmailAddress": "a@b.com"
          |}""".stripMargin
      val user = UserRow.toUser(UserRow(identityId = "A1", brazeId = Some("B2"), jdoc))
      user shouldBe User(
        identityId = "A1",
        brazeId = Some("B2"),
        emailAddress = Some("a@b.com"),
        emailValidated = Some(true),
        userName = Some("abcdef"),
        name = Name(None, Some("Testing"), Some("It")),
        address = Address(
          Some("Guardian News and Media"),
          Some("90 York Way"),
          Some("London"),
          None,
          Some("N1 9GU"),
          Some("India")
        ),
        phoneNumber = Some(PhoneNumber(91, "12345")),
        registrationLocation = Some("wherever"),
        permissions = Seq(Permission("sms", false), Permission("digital_subscriber_preview", true))
      )
    }
  }
}
