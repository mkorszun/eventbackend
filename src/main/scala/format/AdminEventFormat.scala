package format

import model.admin.AdminEvent
import spray.json.DefaultJsonProtocol

object AdminEventJsonFormat extends DefaultJsonProtocol {
    implicit val publicUserFormat = PublicUserJsonProtocol.publicUserFormat
    implicit val adminEventFormat = jsonFormat10(AdminEvent)
}
