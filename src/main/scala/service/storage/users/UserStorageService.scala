package service.storage.users

import java.util.concurrent.Executors

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.{Imports, MongoDBObject}
import model.token.Token
import model.user.{PublicUser, User}
import service.storage.events.EventStorageService
import service.storage.utils.Storage

import scala.concurrent.{ExecutionContext, Future}

object UserStorageService extends Storage {

    val collection = getCollection("users")
    val executorService = Executors.newFixedThreadPool(10)
    val eventStorageService = EventStorageService
    implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

    // Public API ====================================================================================================//

    def createUser(user: User): Token = {
        val query = MongoDBObject("provider_id" -> user.provider_id, "provider" -> user.provider)
        val setOnInsert = MongoDBObject("$setOnInsert" -> userToDocument(user))
        val doc = collection.findAndModify(query, null, null, false, setOnInsert, true, true)
        return tokenFromDocument(doc)
    }

    def updateUser(id: String, token: String, user: PublicUser): Option[PublicUser] = {

        val update = $set(
            "first_name" -> user.first_name,
            "last_name" -> user.last_name,
            "bio" -> user.bio,
            "telephone" -> user.telephone,
            "www" -> user.www,
            "email" -> user.email
        )

        val query: Imports.DBObject = MongoDBObject("_id" -> id, "token" -> token)
        val doc = collection.findAndModify(query, null, null, false, update, true, false)
        if (doc != null) Option(publicUserFromDocument(doc)) else None
    }

    def updatePhoto(id: String, token: String, photo_url: String): Option[PublicUser] = {
        val update = $set("photo_url" -> photo_url)
        val query: Imports.DBObject = MongoDBObject("_id" -> id, "token" -> token)
        val doc = collection.findAndModify(query, null, null, false, update, true, false)
        if (doc != null) Option(publicUserFromDocument(doc)) else None
    }

    def readPrivateUserData(token: String): Option[User] = {
        val doc = collection.findOne(MongoDBObject("token" -> token))
        if (doc != null) Option(userFromDocument(doc)) else None
    }

    def readPublicUserData(id: String): Option[PublicUser] = {
        val doc = collection.findOne(MongoDBObject("_id" -> id))
        if (doc != null) Option(publicUserFromDocument(doc)) else None
    }

    def updateUserData(id: String, user: PublicUser) = Future {
        eventStorageService.updateOwnerData(id, user)
        eventStorageService.updateParticipantsData(id, user)
    }

    def updateUserDevice(id: String, token: String): Array[String] = {
        val query = MongoDBObject("_id" -> id)
        val update = MongoDBObject("$addToSet" -> MongoDBObject("devices" -> token))
        val devices: AnyRef = collection.findAndModify(query, null, null, false, update, true, false).get("devices")
        toArray(devices.asInstanceOf[BasicDBList])
    }

    def getUserDevices(id: String): Array[String] = {
        val query = MongoDBObject("_id" -> id)
        val user = collection.findOne(query)
        val devices: AnyRef = user.get("devices")
        toArray(devices.asInstanceOf[BasicDBList])
    }

    // DB document objects ===========================================================================================//

    def userToDocument(user: User): DBObject = {
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
            "email" -> user.email
        )
    }

    def userToPublicDocument(user: User): DBObject = {
        MongoDBObject(
            "id" -> user.id,
            "first_name" -> user.first_name,
            "last_name" -> user.last_name,
            "photo_url" -> user.photo_url,
            "bio" -> user.bio,
            "telephone" -> user.telephone,
            "www" -> user.www,
            "email" -> user.email
        )
    }

    def userToParticipantDocument(user: User): DBObject = {
        MongoDBObject(
            "id" -> user.id,
            "photo_url" -> user.photo_url,
            "devices" -> user.devices
        )
    }

    def userFromDocument(doc: DBObject): User = {
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
            Option(doc.get("email").asInstanceOf[String]),
            Option(toArray(doc.get("devices").asInstanceOf[BasicDBList]))
        )
    }

    def publicUserFromDocument(doc: DBObject): PublicUser = {
        PublicUser(
            Option(doc.get("_id").toString),
            doc.get("first_name").toString,
            doc.get("last_name").toString,
            Option(doc.get("photo_url").toString),
            doc.get("bio").toString,
            Option(doc.get("telephone").asInstanceOf[String]),
            Option(doc.get("www").asInstanceOf[String]),
            Option(doc.get("email").asInstanceOf[String])
        )
    }

    def publicUserToDocument(user: PublicUser): DBObject = {
        MongoDBObject(
            "id" -> user.id,
            "first_name" -> user.first_name,
            "last_name" -> user.last_name,
            "photo_url" -> user.photo_url,
            "bio" -> user.bio,
            "telephone" -> user.telephone,
            "www" -> user.www,
            "email" -> user.email
        )
    }

    def tokenFromDocument(doc: DBObject): Token = {
        Token(
            doc.get("_id").toString,
            doc.get("token").toString
        )
    }

    //================================================================================================================//
}
