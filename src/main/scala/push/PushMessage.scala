package push

import spray.json.DefaultJsonProtocol

case class PushMessage(default: String, GCM: String, APNS: String, APNS_SANDBOX: String)

case class GCM(data: DATA)

case class APNS(aps: APS, params: Params)

case class APS(content_available: Int, alert: String, sound: String)

case class DATA(message: String, params: Params)

case class Params(event_id: String, event_name: String, msg_type: String, user_name: String, updated_at: Long,
    comments_count: Long, timestamp: Long)

object Params extends DefaultJsonProtocol {
    implicit val format = jsonFormat7(Params.apply)
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
    implicit val format = jsonFormat4(PushMessage.apply)
}
