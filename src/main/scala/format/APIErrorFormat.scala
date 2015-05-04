package format

import spray.json.DefaultJsonProtocol

object ErrorJsonFormat extends DefaultJsonProtocol {
    implicit val apiErrorFormat = jsonFormat1(model.APIError)
}

