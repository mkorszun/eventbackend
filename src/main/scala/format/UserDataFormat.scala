package format

import spray.json.DefaultJsonProtocol

object UserDataJsonFormat extends DefaultJsonProtocol {
    implicit val userDataFormat = jsonFormat6(model.UserData)
}
