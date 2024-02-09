package services

import scala.concurrent.Future

trait BackendService {

  /** If service is healthy, returns future success. Otherwise future failure. This should be an extremely lightweight
    * call as it will be called very often!
    */
  def healthCheck(): Future[Unit]
}
