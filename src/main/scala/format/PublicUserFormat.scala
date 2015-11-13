package format

import model.user.{PublicUser, UserDeviceSettings}
import spray.json.DefaultJsonProtocol

object PublicUserJsonProtocol extends DefaultJsonProtocol {
    implicit val userDeviceSettingsFormat = jsonFormat4(UserDeviceSettings.apply)
    implicit val publicUserFormat = jsonFormat9(PublicUser.apply)
}
