package model.user

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject

case class PublicUser(
    id: Option[String],
    first_name: String,
    last_name: Option[String],
    photo_url: Option[String],
    bio: Option[String],
    telephone: Option[String],
    www: Option[String],
    email: Option[String],
    settings: Option[UserDeviceSettings])

object PublicUser {
    def fromDocument(doc: DBObject): PublicUser = {
        PublicUser(
            Option(doc.get("_id").toString),
            doc.get("first_name").toString,
            Option(doc.get("last_name").toString),
            Option(doc.get("photo_url").toString),
            Option(doc.get("bio").toString),
            Option(doc.get("telephone").asInstanceOf[String]),
            Option(doc.get("www").asInstanceOf[String]),
            Option(doc.get("email").asInstanceOf[String]),
            Option(UserDeviceSettings.fromDocument(doc.get("settings").asInstanceOf[DBObject]))
        )
    }

    def toDocument(user: PublicUser): DBObject = {
        MongoDBObject(
            "id" -> user.id,
            "first_name" -> user.first_name,
            "last_name" -> user.last_name,
            "photo_url" -> user.photo_url,
            "bio" -> user.bio,
            "telephone" -> user.telephone,
            "www" -> user.www,
            "email" -> user.email,
            "settings" -> UserDeviceSettings.toDocument(user.settings.get)
        )
    }
}
