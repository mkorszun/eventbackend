package format

import model.token.Token
import spray.json.DefaultJsonProtocol

object TokenJsonProtocol extends DefaultJsonProtocol {
    implicit val tokenFormat = jsonFormat2(Token)
}
