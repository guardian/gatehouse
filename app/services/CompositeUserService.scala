package services

import model.User

import scala.concurrent.{ExecutionContext, Future}

class CompositeUserService(okta: UserService, identityDb: LegacyUserService)(implicit ctx: ExecutionContext)
    extends UserService {

  def healthCheck(): Future[Unit] = for {
    _ <- okta.healthCheck()
    _ <- identityDb.healthCheck()
  } yield ()

  def fetchUserByIdentityId(id: String): Future[Option[User]] = for {
    optOktaUser <- okta.fetchUserByIdentityId(id)
    optLegacyUser <- identityDb.fetchByIdentityId(id)
  } yield {
    (optOktaUser, optLegacyUser) match {
      case (Some(oktaUser), Some(legacyUser)) =>
        Some(oktaUser.copy(userName = legacyUser.userName, permissions = legacyUser.permissions))
      case (Some(oktaUser), None) => Some(oktaUser)
      case _                      => None
    }
  }
}
