package format

import model.event.Comment
import spray.json.DefaultJsonProtocol

object CommentJsonProtocol extends DefaultJsonProtocol {
    implicit val commentFormat = jsonFormat1(Comment)
}
