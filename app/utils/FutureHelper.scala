package utils

import io.opentelemetry.context.Context as TelemetryContext

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object FutureHelper {

  def tryAsync[A](a: => A)(implicit ctx: ExecutionContext): Future[A] = {
    val telemetryContext = TelemetryContext.current()
    Future(Try {
      telemetryContext.makeCurrent()
      a
    }) flatMap {
      case Success(a)         => Future.successful(a)
      case Failure(exception) => Future.failed(exception)
    }
  }
}
