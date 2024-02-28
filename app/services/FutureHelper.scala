package services

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.util.{Failure, Success, Try}

object FutureHelper {

  def tryAsync[A](a: => A)(implicit ctx: ExecutionContext): Future[A] =
    Future(Try(a)) flatMap {
      case Success(a)         => Future.successful(a)
      case Failure(exception) => Future.failed(exception)
    }
}
