package model.token

import com.mongodb.DBObject
import model.user.User

case class Token(id: String, token: String)

object Token {
    def fromDocument(doc: DBObject): Token = {
        Token(doc.get("_id").toString, doc.get("token").toString)
    }

    def fromUser(user: User): Token = {
        Token(user.id, user.token)
    }
}
