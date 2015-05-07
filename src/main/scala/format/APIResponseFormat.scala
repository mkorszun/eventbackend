package format

import spray.json.DefaultJsonProtocol

object APIResponseFormat extends DefaultJsonProtocol {
    implicit val apiResponseFormat = jsonFormat1(model.APIResponse)
}


