package logging

import org.scalatestplus.play.*
import play.api.mvc.Results.Ok
import play.api.test.*
import play.api.test.Helpers.*

class LogEntrySpec extends PlaySpec {

  "requestAndResponse" should {
    val request = FakeRequest(GET, "/").withHeaders(REFERER -> "Referrer")
    val entry = LogEntry.requestAndResponse(request, response = Ok.withHeaders(CONTENT_LENGTH -> "11"), duration = 7)
    "give correct message" in {
      entry.message mustBe """127.0.0.1 - "GET / HTTP/1.1" 200 11 "Referrer" 7ms"""
    }
    "give correct fields" in {
      entry.otherFields mustBe Map(
        "type" -> "access",
        "origin" -> "127.0.0.1",
        "referrer" -> "Referrer",
        "method" -> "GET",
        "status" -> 200,
        "duration" -> 7,
        "protocol" -> "HTTP/1.1",
        "requested_uri" -> "/",
        "content_length" -> 11
      )
    }
  }

  "error" should {
    val request = FakeRequest(GET, "/").withHeaders(REFERER -> "Referrer")
    val entry = LogEntry.error(request, duration = 17)
    "give correct message" in {
      entry.message mustBe """127.0.0.1 - "GET / HTTP/1.1" ERROR "Referrer" 17ms"""
    }
    "give correct fields" in {
      entry.otherFields mustBe Map(
        "type" -> "access",
        "origin" -> "127.0.0.1",
        "referrer" -> "Referrer",
        "method" -> "GET",
        "duration" -> 17,
        "protocol" -> "HTTP/1.1",
        "requested_uri" -> "/",
      )
    }
  }
}
