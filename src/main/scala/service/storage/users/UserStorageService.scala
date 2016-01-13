package service.storage.users

import java.util.concurrent.Executors

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.{Imports, MongoDBObject}
import com.mongodb.{CommandFailureException, DBObject, DuplicateKeyException}
import model.user.{PublicUser, User, UserDeviceSettings}
import push.PushType
import push.PushType.PushType
import service.storage.auth.EmailAlreadyExists
import service.storage.events.EventStorageService
import service.storage.utils.Storage

import scala.concurrent.{ExecutionContext, Future}

object UserStorageService extends Storage {

    val collection = getCollection("users")
    val executorService = Executors.newFixedThreadPool(10)
    val eventStorageService = EventStorageService
    implicit val executionContext = ExecutionContext.fromExecutorService(executorService)

    // Public API ====================================================================================================//

    def updateUser(id: String, token: String, user: PublicUser): Option[PublicUser] = {

        try {

            val update = MongoDBObject(
                "first_name" -> user.first_name,
                "last_name" -> user.last_name,
                "bio" -> user.bio,
                "telephone" -> user.telephone,
                "www" -> user.www,
                "settings" -> UserDeviceSettings.toDocument(user.settings.get)
            )

            val update2 = if (user.email.isDefined && !user.email.get.isEmpty) {
                update.put("email", user.email)
                MongoDBObject("$set" -> update)
            } else {
                MongoDBObject("$set" -> update) ++ $unset("email")
            }

            val query: Imports.DBObject = MongoDBObject("_id" -> id, "token" -> token)
            val doc = collection.findAndModify(query, null, null, false, update2, true, false)
            if (doc != null) Option(PublicUser.fromDocument(doc)) else None
        } catch {
            case _: DuplicateKeyException | _: CommandFailureException =>
                throw new EmailAlreadyExists
        }
    }

    def updatePhoto(id: String, token: String, photo_url: String): Option[PublicUser] = {
        val update = $set("photo_url" -> photo_url)
        val query: Imports.DBObject = MongoDBObject("_id" -> id, "token" -> token)
        val doc = collection.findAndModify(query, null, null, false, update, true, false)
        if (doc != null) Option(PublicUser.fromDocument(doc)) else None
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

    def getUserDevices(ids: Array[String], setting_type: PushType): Array[String] = {
        val steps: java.util.List[DBObject] = aggregationSteps(Array(
            MongoDBObject(
                "$match" -> MongoDBObject("_id" -> MongoDBObject("$in" -> ids), getSetting(setting_type) -> true)),
            MongoDBObject("$project" -> MongoDBObject("a" -> "$devices")),
            MongoDBObject("$unwind" -> "$a"),
            MongoDBObject("$group" -> new Group("$a", "all")))
        )
        return toArray(collection.aggregate(steps, AggregationOptions(AggregationOptions.CURSOR)))
    }

    def getUserDevices(ids: Array[String], setting_type: PushType, regex: String): Array[String] = {
        val steps: java.util.List[DBObject] = aggregationSteps(Array(
            MongoDBObject(
                "$match" ->
                    MongoDBObject("_id" -> MongoDBObject("$in" -> ids), getSetting(setting_type) -> true,
                        "devices" -> MongoDBObject("$elemMatch" -> MongoDBObject("$regex" -> regex)))),
            MongoDBObject("$project" -> MongoDBObject("a" -> "$devices")),
            MongoDBObject("$unwind" -> "$a"),
            MongoDBObject("$group" -> new Group("$a", "all")))
        )
        return toArray(collection.aggregate(steps, AggregationOptions(AggregationOptions.CURSOR)))
    }

    def getUserDevices(ids: Array[String], regex: String): Array[String] = {
        val steps: java.util.List[DBObject] = aggregationSteps(Array(
            MongoDBObject(
                "$match" ->
                    MongoDBObject("_id" -> MongoDBObject("$in" -> ids),
                        "devices" -> MongoDBObject("$elemMatch" -> MongoDBObject("$regex" -> regex)))),
            MongoDBObject("$project" -> MongoDBObject("a" -> "$devices")),
            MongoDBObject("$unwind" -> "$a"),
            MongoDBObject("$group" -> new Group("$a", "all")))
        )
        return toArray(collection.aggregate(steps, AggregationOptions(AggregationOptions.CURSOR)))
    }

    def removeDevice(arn: String): Unit = {
        collection
            .findAndModify(MongoDBObject("devices" -> MongoDBObject("$in" -> Array(arn))), null, null,
                false, MongoDBObject("$pull" -> MongoDBObject("devices" -> arn)), true, false)
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

    def getSetting(msg: PushType): String = msg match {
        case PushType.new_participant => "settings.push_on_new_participant"
        case PushType.new_comment => "settings.push_on_new_comment"
        case PushType.event_updated => "settings.push_on_update"
        case PushType.leaving_participant => "settings.push_on_leaving_participant"
    }

    //================================================================================================================//
}
