package format

import model.event.Event
import spray.json.DefaultJsonProtocol

object EventJsonFormat extends DefaultJsonProtocol {
    implicit val eventFormat = jsonFormat11(Event)
}
