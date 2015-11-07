package push

import spray.json.DefaultJsonProtocol

case class PushMessage(event_id: String, event_name: String, msg_type: String)

object PushMessage extends DefaultJsonProtocol {
    implicit val pushMessageFormat = jsonFormat3(PushMessage.apply)
}