package format

import model.user.PublicUser
import spray.json.DefaultJsonProtocol

object PublicUserJsonProtocol extends DefaultJsonProtocol {
    implicit val publicUserFormat = jsonFormat5(PublicUser)
}
