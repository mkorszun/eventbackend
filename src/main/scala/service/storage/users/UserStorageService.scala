package service.storage.users

import java.util.concurrent.Executors

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.{Imports, MongoDBObject}
import model.token.Token
import model.user.{PublicUser, User, UserDeviceSettings}
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
        val setOnInsert = MongoDBObject("$setOnInsert" -> User.toDocument(user))
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
            "email" -> user.email,
            "settings" -> UserDeviceSettings.toDocument(user.settings.get)
        )

        val query: Imports.DBObject = MongoDBObject("_id" -> id, "token" -> token)
        val doc = collection.findAndModify(query, null, null, false, update, true, false)
        if (doc != null) Option(PublicUser.fromDocument(doc)) else None
    }

    def updatePhoto(id: String, token: String, photo_url: String): Option[PublicUser] = {
        val update = $set("photo_url" -> photo_url)
        val query: Imports.DBObject = MongoDBObject("_id" -> id, "token" -> token)
        val doc = collection.findAndModify(query, null, null, false, update, true, false)
        if (doc != null) Option(PublicUser.fromDocument(doc)) else None
    }

    def readPrivateUserData(token: String): Option[User] = {
        val doc = collection.findOne(MongoDBObject("token" -> token))
        if (doc != null) Option(User.fromDocument(doc)) else None
    }

    def readPublicUserData(id: String): Option[PublicUser] = {
        val doc = collection.findOne(MongoDBObject("_id" -> id))
        if (doc != null) Option(PublicUser.fromDocument(doc)) else None
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

    def getUserDevices(ids: Array[String], setting_type: String): Array[String] = {
        val steps: java.util.List[DBObject] = aggregationSteps(Array(
            MongoDBObject("$match" -> MongoDBObject("_id" -> MongoDBObject("$in" -> ids), getSetting(setting_type) -> true)),
            MongoDBObject("$project" -> MongoDBObject("a" -> "$devices")),
            MongoDBObject("$unwind" -> "$a"),
            MongoDBObject("$group" -> new Group("$a", "all")))
        )
        return toArray(collection.aggregate(steps, AggregationOptions(AggregationOptions.CURSOR)))
    }

    // DB document objects ===========================================================================================//

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

    def tokenFromDocument(doc: DBObject): Token = {
        Token(
            doc.get("_id").toString,
            doc.get("token").toString
        )
    }

    def getSetting(msg: String): String = msg match {
        case "new_participant" => "settings.push_on_new_participant"
        case "new_comment" => "settings.push_on_new_comment"
        case "event_updated" => "settings.push_on_update"
        case "leaving_participant" => "settings.push_on_leaving_participant"
    }

    //================================================================================================================//
}
