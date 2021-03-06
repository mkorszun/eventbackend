package model.user

import java.util.Date

import auth.BearerTokenGenerator
import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import org.mindrot.jbcrypt.BCrypt
import service.storage.users.UserStorageService._

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
    devices: Option[Array[String]],
    password: Option[String],
    verified: Boolean,
    confirmation_token: Option[String]) {

    def fullName: String = f"$first_name $last_name"
}

object User {
    def toDocument(user: User): DBObject = {
        val doc = MongoDBObject(
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
            "devices" -> user.devices,
            "settings" -> UserDeviceSettings.toDocument(new UserDeviceSettings(true, true, true)),
            "password" -> user.password,
            "verified" -> user.verified,
            "confirmation_token" -> user.confirmation_token
        )

        if (!user.verified) doc.put("unverified_since", new Date())
        if (user.email.isDefined && !user.email.get.isEmpty) doc.put("email", user.email)
        return doc
    }

    def toParticipantDocument(user: User): DBObject = {
        MongoDBObject(
            "id" -> user.id,
            "photo_url" -> user.photo_url,
            "devices" -> user.devices
        )
    }

    def fromDocument(doc: DBObject): User = {
        val email: Option[String] = if (doc.contains("email")) {
            Option(doc.get("email").asInstanceOf[String])
        } else {
            None
        }

        User(
            doc.get("_id").toString,
            doc.get("provider_id").toString,
            doc.get("provider").toString,
            doc.get("token").toString,
            doc.get("first_name").toString,
            doc.get("last_name").toString,
            doc.get("photo_url").toString,
            doc.get("bio").toString,
            Option(doc.get("telephone").asInstanceOf[String]),
            Option(doc.get("www").asInstanceOf[String]),
            email,
            Option(toArray(doc.get("devices").asInstanceOf[BasicDBList])),
            Option(doc.get("password").toString),
            doc.getAs[Boolean]("verified").getOrElse(true),
            None
        )
    }

    def fromEmailPassword(email: String, password: String, photo: String): User = {
        val userToken = BearerTokenGenerator.generateSHAToken(email)
        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
        val id: String = java.util.UUID.randomUUID.toString
        User(id, "", "email", userToken, "", "", photo, "", None, None, Option(email), Option(Array()),
            Option(passwordHash), false, Option(BearerTokenGenerator.generateSHAToken(id)))
    }
}
