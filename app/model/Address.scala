package model

case class Address(
    line1: Option[String],
    line2: Option[String],
    line3: Option[String],
    line4: Option[String],
    postcode: Option[String],
    country: Option[String],
)
