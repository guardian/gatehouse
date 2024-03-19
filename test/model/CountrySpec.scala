package model

import org.scalatestplus.play.PlaySpec

class CountrySpec extends PlaySpec {
  "getCountryByCode" should {

    "Find all countries by their declared code" in {
      Country.values.foreach { country =>
        Country.getCountryByCode(country.code) mustBe (Some(country))
      }
    }
    "Some examples match can be found by their expected codes" in {
      Country.getCountryByCode("GB") mustBe Some(Country.GB)
      Country.getCountryByCode("US") mustBe Some(Country.US)
      Country.getCountryByCode("FR") mustBe Some(Country.FR)
      Country.getCountryByCode("CA") mustBe Some(Country.CA)
      Country.getCountryByCode("AR") mustBe Some(Country.AR)
    }
    "Invalid country codes return None" in {
      Country.getCountryByCode("ZZ") mustBe None
    }
  }
  "getCountryByName" should {

    "Find all countries by their declared name" in {
      Country.values.foreach { country =>
        Country.getCountryByName(country.name) mustBe (Some(country))
      }
    }
    "Some examples match can be found by their expected names" in {
      Country.getCountryByName("United Kingdom") mustBe Some(Country.GB)
      Country.getCountryByName("United States") mustBe Some(Country.US)
      Country.getCountryByName("France") mustBe Some(Country.FR)
      Country.getCountryByName("Canada") mustBe Some(Country.CA)
      Country.getCountryByName("Argentina") mustBe Some(Country.AR)
    }
    "Unknown country names return None" in {
      Country.getCountryByName("Narnia") mustBe None
    }
  }

}
