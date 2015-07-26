package directives

import model.user.User
import spray.routing.Directive1
import spray.routing.Directives._

class UnauthorizedException extends Exception

trait UserPermissions {

    def checkPermissions(user_id: String, user: User): Directive1[Boolean] = {
        val isSelf: Boolean = user_id == user.id
        if (!isSelf) throw new UnauthorizedException
        provide(isSelf)
    }
}