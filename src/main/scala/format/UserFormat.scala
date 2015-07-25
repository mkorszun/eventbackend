package format

import model.user.User
import spray.json.DefaultJsonProtocol

object UserJsonProtocol extends DefaultJsonProtocol {
    implicit val userFormat = jsonFormat9(User)
}