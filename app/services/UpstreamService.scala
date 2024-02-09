package services

import scala.concurrent.Future

trait UpstreamService {

  /** If service is healthy, returns future success. Otherwise future failure. This should be extremely lightweight and
    * have no limits on how often it's called as it will be called very often!
    */
  def healthCheck(): Future[Unit]
}
