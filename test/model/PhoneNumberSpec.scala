package model

import org.scalatestplus.play.PlaySpec

class PhoneNumberSpec extends PlaySpec {

  "fromPhoneNumberString" should {
    "return None for an empty string" in {
      PhoneNumber.fromPhoneNumberString("") mustBe None
    }
    "return None for a number with no country code" in {
      PhoneNumber.fromPhoneNumberString("1234567890") mustBe None
    }
    "return None for a number with no local number" in {
      PhoneNumber.fromPhoneNumberString("+12") mustBe None
    }
    "return None for a string not beginning with '+'" in {
      PhoneNumber.fromPhoneNumberString("12 34567890") mustBe None
    }
    "return a PhoneNumber for a string beginning with '+'" in {
      PhoneNumber.fromPhoneNumberString("+12   345-67-8(9) ") mustBe Some(PhoneNumber(12, "345-67-8(9)"))
    }
  }
}
