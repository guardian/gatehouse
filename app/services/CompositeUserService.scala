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

  def fetchUserByIdentityId(identityId: String): Future[Option[User]] = for {
    optLegacyUser <- identityDb.fetchUserByIdentityId(identityId)
    optOktaUser <- optLegacyUser.flatMap(_.oktaId.map(okta.fetchUserByOktaId)).getOrElse(Future.successful(None))
  } yield for {
    oktaUser <- optOktaUser
    legacyUser <- optLegacyUser
  } yield oktaUser.copy(userName = legacyUser.userName, permissions = legacyUser.permissions)
}
