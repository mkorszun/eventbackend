package format

import spray.json.DefaultJsonProtocol

object UserDataJsonFormat extends DefaultJsonProtocol {
    implicit val userDataFormat = jsonFormat7(model.UserData)
}
