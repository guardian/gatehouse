package services

import scala.concurrent.Future

trait LegacyUserService extends Service {

  def fetchByIdentityId(id: String): Future[Option[LegacyUser]]
}
