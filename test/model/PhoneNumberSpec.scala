package model

import org.scalatestplus.play.PlaySpec

class PhoneNumberSpec extends PlaySpec {

  "fromPhoneNumberString" should {
    "return None for an empty string" in {
      PhoneNumber.fromPhoneNumberString("", None) mustBe None
    }
    "return None for a number with no country code" in {
      PhoneNumber.fromPhoneNumberString("1234567890", None) mustBe None
    }
    "return None for a number with no local number" in {
      PhoneNumber.fromPhoneNumberString("+12", None) mustBe None
    }
    "return None for a string not beginning with '+'" in {
      PhoneNumber.fromPhoneNumberString("12 34567890", None) mustBe None
    }
    "return a PhoneNumber for a string beginning with '+'" in {
      PhoneNumber.fromPhoneNumberString("+91   345-67-8(9) ", None) mustBe Some(PhoneNumber(91, "3456789"))
    }
  }
}
