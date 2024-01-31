package logging

import play.api.http.HeaderNames.*
import play.api.mvc.{RequestHeader, Result}

case class LogEntry(message: String, otherFields: Map[String, Any])

private[logging] object LogEntry {

  private def commonFields(request: RequestHeader, duration: Long) =
    Map(
      "type" -> "access",
      "origin" -> request.headers.get(X_FORWARDED_FOR).getOrElse(request.remoteAddress),
      "referrer" -> request.headers.get(REFERER).getOrElse(""),
      "method" -> request.method,
      "duration" -> duration,
      "protocol" -> request.version,
      "requested_uri" -> request.uri,
    )

  def requestAndResponse(request: RequestHeader, response: Result, duration: Long): LogEntry = {
    val fields = commonFields(request, duration) ++ Map(
      "status" -> response.header.status,
      "content_length" -> response.header.headers.getOrElse(CONTENT_LENGTH, 0),
      "content_length" -> response.header.headers.get(CONTENT_LENGTH).map(_.toInt).getOrElse(0),
    )
    val message =
      s"""${fields("origin")} -
           |"${request.method} ${request.uri} ${request.version}"
           |${response.header.status}
           |${fields("content_length")}
           |"${fields("referrer")}"
           |${duration}ms""".stripMargin.replaceAll("\n", " ")
    LogEntry(message, fields)
  }

  def error(request: RequestHeader, duration: Long): LogEntry = {
    val fields = commonFields(request, duration)
    val message =
      s"""${fields("origin")} -
           |"${request.method} ${request.uri} ${request.version}"
           |ERROR
           |"${fields("referrer")}"
           |${duration}ms""".stripMargin.replaceAll("\n", " ")
    LogEntry(message, fields)
  }
}
