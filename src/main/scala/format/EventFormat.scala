package format

import spray.json.DefaultJsonProtocol

object EventJsonFormat extends DefaultJsonProtocol {
  implicit val locationFormat = jsonFormat4(model.Event)
}
