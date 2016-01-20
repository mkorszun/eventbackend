package format

import model.user.{PublicUser, UserDeviceSettings}
import spray.json._

object PublicUserJsonProtocol extends DefaultJsonProtocol {

    implicit object MyBooleanJsonFormat extends JsonFormat[Boolean] {
        def write(value: Boolean): JsBoolean = {
            return JsBoolean(value)
        }

        def read(value: JsValue) = {
            value match {
                case JsNumber(n) if n == 1 => true
                case JsNumber(n) if n == 0 => false
                case JsBoolean(v) => v
                case _ => throw new DeserializationException("Not a boolean")
            }
        }
    }

    implicit val userDeviceSettingsFormat = jsonFormat4(UserDeviceSettings.apply)
    implicit val publicUserFormat = jsonFormat9(PublicUser.apply)
}
