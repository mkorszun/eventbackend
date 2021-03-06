package auth

import org.mindrot.jbcrypt.BCrypt
import service.storage.auth.AuthStorageService
import spray.routing.authentication.{BasicAuth, UserPass}
import spray.routing.directives.AuthMagnet

import scala.concurrent.{ExecutionContext, Future}

trait BasicAuthenticator {
    def basicUserAuthenticator(implicit ec: ExecutionContext): AuthMagnet[model.user.User] = {
        def validateUser(userPass: Option[UserPass]): Option[model.user.User] = {
            val user = AuthStorageService.loadUserByEmail(userPass.get.user)
            if (user.isDefined && BCrypt.checkpw(userPass.get.pass, user.get.password.get)) return user else None
        }

        def authenticator(userPass: Option[UserPass]): Future[Option[model.user.User]] = Future {
            validateUser(userPass)
        }

        BasicAuth(authenticator _, realm = "Private API")
    }
}
