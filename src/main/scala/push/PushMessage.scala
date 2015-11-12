package push

import spray.json.DefaultJsonProtocol

case class PushMessage(event_id: String, event_name: String, msg_type: String, user_name: String)

case class PushMessageWrapper1(content_available: String, payload: PushMessage)

case class PushMessageWrapper2(aps: PushMessageWrapper1)

object PushMessage extends DefaultJsonProtocol {
    implicit val pushMessageFormat = jsonFormat4(PushMessage.apply)
}

object PushMessageWrapper1 extends DefaultJsonProtocol {
    implicit val pushMessageFormat = jsonFormat2(PushMessageWrapper1.apply)
}

object PushMessageWrapper2 extends DefaultJsonProtocol {
    implicit val pushMessageFormat = jsonFormat1(PushMessageWrapper2.apply)
}