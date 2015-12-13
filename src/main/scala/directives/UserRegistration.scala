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

    def checkCredentials(password: String): Directive1[Boolean] = {
        if (isValidPassword(password)) return provide(true)
        throw new InvalidCredentialsException("Password has to be at least 6 characters long")
    }

    def isValidEmail(email : String): Boolean = if("""^[-a-z0-9!#$%&'*+/=?^_`{|}~]+(\.[-a-z0-9!#$%&'*+/=?^_`{|}~]+)*@([a-z0-9]([-a-z0-9]{0,61}[a-z0-9])?\.)*(aero|arpa|asia|biz|cat|com|coop|edu|gov|info|int|jobs|mil|mobi|museum|name|net|org|pro|tel|travel|[a-z][a-z])$""".r.findFirstIn(email) == None)false else true

    def isValidPassword(password: String): Boolean = password.length >= 8
}
