package model

import slick.jdbc.PostgresProfile.api.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

case class User(identityId: String, brazeId: String)
