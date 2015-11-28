package directives

import model.user.NewUser
import spray.routing.Directives._
import spray.routing._

case class InvalidCredentialsException(msg: String) extends Exception(msg)

trait UserRegistration {

    def checkCredentials(new_user: NewUser): Directive1[Boolean] = {
        if (isValidEmail(new_user.email) && isValidPassword(new_user.password)) return provide(true)
        throw new InvalidCredentialsException("Valid email and password required")
    }

    def isValidEmail(email: String): Boolean = """(\w+)@([\w\.]+)""".r.unapplySeq(email).isDefined

    def isValidPassword(password: String): Boolean = password.length >= 8
}
