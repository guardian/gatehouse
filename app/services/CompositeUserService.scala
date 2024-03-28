package services

import model.User

import scala.concurrent.{ExecutionContext, Future}

class CompositeUserService(okta: OktaUserService, identityDb: LegacyIdentityDbUserService)(implicit
    ctx: ExecutionContext
) extends UserService {

  def healthCheck(): Future[Unit] = for {
    _ <- okta.healthCheck()
    _ <- identityDb.healthCheck()
  } yield ()

  override def fetchUserByOktaId(oktaId: String): Future[Option[User]] = for {
    optOktaUser <- okta.fetchUserByOktaId(oktaId)
    optLegacyUser <- optOktaUser
      .map(oktaUser => identityDb.fetchUserByIdentityId(oktaUser.legacyIdentityId))
      .getOrElse(Future.successful(None))
  } yield for {
    oktaUser <- optOktaUser
    legacyUser <- optLegacyUser
  } yield oktaUser.copy(userName = legacyUser.userName, permissions = legacyUser.permissions)
}
