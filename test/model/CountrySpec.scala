package model

import org.scalatestplus.play.PlaySpec

class CountrySpec extends PlaySpec {
  "getCountryByCode" should {

    "Find all countries by their declared code" in {
      Country.values.foreach { country =>
        Country.findByCode(country.code) mustBe (Some(country))
      }
    }
    "Some examples can be found by their expected codes" in {
      Country.findByCode("GB") mustBe Some(Country.GB)
      Country.findByCode("US") mustBe Some(Country.US)
      Country.findByCode("FR") mustBe Some(Country.FR)
      Country.findByCode("CA") mustBe Some(Country.CA)
      Country.findByCode("AR") mustBe Some(Country.AR)
    }
    "Invalid country codes return None" in {
      Country.findByCode("ZZ") mustBe None
    }
  }
  "getCountryByName" should {

    "Find all countries by their declared name" in {
      Country.values.foreach { country =>
        Country.findByName(country.name) mustBe (Some(country))
      }
    }
    "Some examples can be found by their expected names" in {
      Country.findByName("United Kingdom") mustBe Some(Country.GB)
      Country.findByName("United States") mustBe Some(Country.US)
      Country.findByName("France") mustBe Some(Country.FR)
      Country.findByName("Canada") mustBe Some(Country.CA)
      Country.findByName("Argentina") mustBe Some(Country.AR)
    }
    "Unknown country names return None" in {
      Country.findByName("Narnia") mustBe None
    }
  }

}
