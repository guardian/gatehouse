package utils

object StringHelper {

  /** @return Some(s) if s is non-null and non-empty, otherwise None. */
  def nonNullNonEmpty(s: String): Option[String] =
    Option(s).filter(!_.isBlank)
}
