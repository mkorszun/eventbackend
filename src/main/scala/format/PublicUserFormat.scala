package format

import model.user.{PublicUser, UserDeviceSettings}
import spray.json.DefaultJsonProtocol

object PublicUserJsonProtocol extends DefaultJsonProtocol {
    implicit val userDeviceSettingsFormat = jsonFormat3(UserDeviceSettings.apply)
    implicit val publicUserFormat = jsonFormat9(PublicUser.apply)
}
