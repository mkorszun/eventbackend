package format

import spray.json.DefaultJsonProtocol

object EventJsonFormat extends DefaultJsonProtocol {
    implicit val eventFormat = jsonFormat9(model.Event)
}
