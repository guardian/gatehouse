package controllers

import io.opentelemetry.api.trace.SpanKind.SERVER
import io.opentelemetry.api.trace.{SpanKind, StatusCode, Tracer}
import play.api.mvc.*

import scala.concurrent.ExecutionContext

class TelemetryFilter(tracer: Tracer)(implicit ec: ExecutionContext) extends EssentialFilter {

  override def apply(next: EssentialAction): EssentialAction = request => {
    val span = tracer.spanBuilder(request.path).setSpanKind(SERVER).startSpan()
    span.makeCurrent()
    val accumulator = next(request)
    accumulator
      .map { result =>
        span.end()
        result
      }
      .recover { case e: Exception =>
        span.setStatus(StatusCode.ERROR, e.getMessage)
        span.recordException(e)
        span.end()
        Results.InternalServerError(s"Telemetry failure: ${e.getMessage}")
      }
  }
}
