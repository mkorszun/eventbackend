package model.user

import spray.json.DefaultJsonProtocol

case class NewUser(email: String, password: String)

object NewUserFormat extends DefaultJsonProtocol {
    implicit val newUserFormat = jsonFormat2(NewUser)
}
