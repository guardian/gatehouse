package logging

import net.logstash.logback.marker.Markers.appendEntries
import org.apache.pekko.stream.Materializer
import play.api.mvc.{Filter, RequestHeader, Result}
import play.api.{Logging, MarkerContext}
import logging.LogEntry
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success}

/** Logs all requests and responses. Fields logged:
  *   - type: always "access" to distinguish it from an app log entry
  *   - origin: the IP address of the client
  *   - referrer: the referrer header
  *   - method: the HTTP method
  *   - status: the HTTP status code
  *   - duration: the duration of the request in milliseconds
  *   - protocol: the HTTP protocol version
  *   - requested_uri: the requested URI surprisingly
  *   - content_length: the length of the response body
  *   - message: summary for a successful request or error and stacktrace if request failed
  *
  * Largely stolen from
  * https://github.com/guardian/cdk-playground/blob/02e91848c5c70f72c281a02a5f7107c6de0298d4/app/RequestLoggingFilter.scala
  */
class RequestLoggingFilter(override val mat: Materializer)(implicit ec: ExecutionContext) extends Filter with Logging {

  override def apply(next: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {
    val start = System.currentTimeMillis()
    val result = next(request)

    result onComplete {
      case Success(response) =>
        val duration = System.currentTimeMillis() - start
        logSuccess(LogEntry.requestAndResponse(request, response, duration))

      case Failure(err) =>
        val duration = System.currentTimeMillis() - start
        logFailure(request, err, LogEntry.error(request, duration))
    }

    result
  }

  private def logSuccess(logEntry: LogEntry): Unit =
    logger.info(logEntry.message)(MarkerContext(appendEntries(logEntry.otherFields.asJava)))

  private def logFailure(request: RequestHeader, throwable: Throwable, logEntry: LogEntry): Unit = {
    logger.info(logEntry.message)(MarkerContext(appendEntries(logEntry.otherFields.asJava)))
    logger.error(s"Error for ${request.method} ${request.uri}", throwable)
  }
}
