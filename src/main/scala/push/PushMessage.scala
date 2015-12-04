package push

import spray.json.DefaultJsonProtocol

case class PushMessage(default: String, GCM: String, APNS: String)

case class GCM(data: DATA)

case class APNS(aps: APS, params: Params)

case class APS(content_available: Int, alert: String, sound: String)

case class DATA(message: String, params: Params)

case class Params(event_id: String, event_name: String, msg_type: String, user_name: String)

object Params extends DefaultJsonProtocol {
    implicit val format = jsonFormat4(Params.apply)
}

object DATA extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(DATA.apply)
}

object APS extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(APS.apply)
}

object GCM extends DefaultJsonProtocol {
    implicit val format = jsonFormat1(GCM.apply)
}

object APNS extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(APNS.apply)
}

object PushMessage extends DefaultJsonProtocol {
    implicit val format = jsonFormat3(PushMessage.apply)
}
