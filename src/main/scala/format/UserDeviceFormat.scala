package format

import model.user.UserDevice
import spray.json._

object UserDeviceJsonProtocol extends DefaultJsonProtocol {
    implicit val userDeviceFormat = jsonFormat2(UserDevice)
}
