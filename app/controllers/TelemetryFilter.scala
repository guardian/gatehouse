package controllers

import io.opentelemetry.api.trace.{StatusCode, TracerProvider}
import play.api.mvc.*

import scala.concurrent.ExecutionContext

class TelemetryFilter(tracerProvider: TracerProvider)(implicit ec: ExecutionContext) extends EssentialFilter {

  private val tracerName = "TelemetryFilter"

  override def apply(next: EssentialAction): EssentialAction = request => {
    val tracer = tracerProvider.get(tracerName)
    val span = tracer.spanBuilder(request.path).startSpan()
    val scope = span.makeCurrent()
    val accumulator = next(request)
    accumulator
      .map { result =>
        span.end()
        scope.close()
        result
      }
      .recover { case e: Exception =>
        span.setStatus(StatusCode.ERROR, e.getMessage)
        span.recordException(e)
        span.end()
        scope.close()
        Results.InternalServerError(s"Telemetry failure: ${e.getMessage}")
      }
  }
}
