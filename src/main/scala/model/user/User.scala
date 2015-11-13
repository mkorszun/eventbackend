package model.user

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject

case class User(
    id: String,
    provider_id: String,
    provider: String,
    token: String,
    first_name: String,
    last_name: String,
    photo_url: String,
    bio: String,
    telephone: Option[String],
    www: Option[String],
    email: Option[String],
    devices: Option[Array[String]]) {

    def fullName: String = f"$first_name $last_name"
}

object User {
    def toDocument(user: User): DBObject = {
        MongoDBObject(
            "_id" -> user.id,
            "provider_id" -> user.provider_id,
            "provider" -> user.provider,
            "token" -> user.token,
            "first_name" -> user.first_name,
            "last_name" -> user.last_name,
            "photo_url" -> user.photo_url,
            "bio" -> user.bio,
            "telephone" -> user.telephone,
            "www" -> user.www,
            "email" -> user.email,
            "devices" -> user.devices,
            "settings" -> UserDeviceSettings.toDocument(new UserDeviceSettings(true, true, true))
        )
    }
}
