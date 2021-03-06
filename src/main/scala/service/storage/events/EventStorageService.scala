package service.storage.events

import java.util.{Calendar, Date}

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.{Imports, MongoDBList, MongoDBListBuilder, MongoDBObject}
import com.mongodb.{Cursor, DBCursor}
import model.event.Event
import model.user.{PublicUser, User}
import service.storage.users.UserStorageService
import service.storage.utils.Storage

object EventStorageService extends Storage {

    val collection = getCollection("events")

    // Public API ====================================================================================================//

    def saveEvent(user: User, event: Event) {
        collection.insert(new DBEvent(user, event))
    }

    def findEvents(user_id: String, only_timestamp: Option[Boolean]): DBCursor = {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, -15)

        val timestamp = MongoDBObject("$gte" -> calendar.getTime().getTime)
        val query = MongoDBObject("participants.id" -> user_id, "timestamp" -> timestamp)
        return collection
            .find(query, if (only_timestamp.getOrElse(false)) EVENT_LIST_TIMESTAMPS else EVENT_LIST_FIELDS)
            .sort(MongoDBObject("timestamp" -> 1))
    }

    def findEvents(x: Double, y: Double, max: Long, phrases: Array[String]): DBCursor = {
        val geo = MongoDBObject("$geometry" -> new DBGeoPoint(x, y), "$maxDistance" -> max)
        val timestamp = MongoDBObject("$gte" -> Calendar.getInstance().getTime().getTime)
        val query = MongoDBObject("loc" -> MongoDBObject("$near" -> geo), "timestamp" -> timestamp)
        if (phrases.length > 0) query.put("tags", MongoDBObject("$regex" -> tags(phrases), "$options" -> "i"))
        return collection.find(query, EVENT_LIST_FIELDS).sort(MongoDBObject("timestamp" -> 1))
    }

    def getEvent(event_id: String): DBObject = {
        val query = MongoDBObject("_id" -> event_id)
        val doc = collection.findOne(query, EVENT_DETAILS_FIELDS)
        if (doc != null) return doc
        throw new EventNotFound
    }

    def getEventComments(event_id: String): DBObject = {
        val query = MongoDBObject("_id" -> event_id)
        val doc = collection.findOne(query, EVENT_COMMENTS_FIELDS)
        if (doc != null) return doc
        throw new EventNotFound
    }

    def removeParticipant(event_id: String, user: User): DBObject = {
        if (!isEvent(event_id)) throw new EventNotFound
        val event = MongoDBObject("_id" -> event_id, "participants.id" -> user.id)
        val participant = MongoDBObject("participants" -> MongoDBObject("id" -> user.id))
        val update = MongoDBObject("$pull" -> participant, "$inc" -> MongoDBObject("spots" -> -1), "$set" -> updated)
        val doc = collection.findAndModify(event, EVENT_DETAILS_FIELDS, null, false, update, true, false)
        if (doc != null) return doc
        throw new UserNotPresent
    }

    def addParticipant(event_id: String, user: User): DBObject = {
        if (!isEvent(event_id)) throw new EventNotFound
        val constraint = MongoDBObject("$nin" -> (MongoDBList.newBuilder[String] += user.id).result())
        val event = MongoDBObject("_id" -> event_id, "participants.id" -> constraint)
        val participant = MongoDBObject("participants" -> User.toParticipantDocument(user))
        val update = MongoDBObject("$addToSet" -> participant, "$inc" -> MongoDBObject("spots" -> 1), "$set" -> updated)
        val doc = collection.findAndModify(event, EVENT_DETAILS_FIELDS, null, false, update, true, false)
        if (doc != null) return doc
        throw new UserAlreadyAdded
    }

    def addComment(event_id: String, user: User, msg: String): DBObject = {
        val event = MongoDBObject("_id" -> event_id, "participants.id" -> user.id)
        val update = MongoDBObject("$push" -> MongoDBObject("comments" -> new DBEventComment(user, msg)),
            "$inc" -> MongoDBObject(COMMENTS_COUNT -> 1))
        val doc = collection.findAndModify(event, EVENT_COMMENTS_FIELDS, null, false, update, true, false)
        if (doc != null) return doc
        throw new EventNotFound
    }

    def updateEvent(event_id: String, user: User, event: Event): DBObject = {
        val update = $set(
            "headline" -> event.headline,
            "description" -> event.description,
            "timestamp" -> event.timestamp,
            "loc" -> new DBGeoPoint(event.x, event.y),
            "tags" -> event.tags,
            "distance" -> event.distance,
            "pace" -> event.pace,
            UPDATED_AT -> new Date().getTime
        )

        val query = MongoDBObject("_id" -> event_id, "user.id" -> user.id)
        val doc = collection.findAndModify(query, EVENT_DETAILS_FIELDS, null, false, update, true, false)
        if (doc != null) return doc
        throw new EventNotFound
    }

    def deleteEvent(event_id: String, user: User): Unit = {
        if (!isEvent(event_id, user)) throw new EventNotFound

        val doc = collection.findAndRemove(MongoDBObject(
            "_id" -> event_id,
            "user.id" -> user.id,
            "participants.id" -> user.id,
            "$where" -> "this.participants.length==1")
        )

        if (doc == null) throw new EventHasOtherParticipants
    }

    def updateOwnerData(id: String, user: PublicUser): Unit = {
        val update = $set("user" -> PublicUser.toDocument(user))
        collection.update(MongoDBObject("user.id" -> id), update, false, true)
    }

    def updateParticipantsData(id: String, user: PublicUser): Unit = {
        val update = $set("participants.$.photo_url" -> user.photo_url)
        collection.update(MongoDBObject("participants.id" -> id), update, false, true)
    }

    def aggregate(steps: java.util.List[DBObject]): Cursor = {
        aggregate(collection, steps)
    }

    // Helpers =======================================================================================================//

    private def isEvent(id: String): Boolean = {
        return if (collection.find(MongoDBObject("_id" -> id)).limit(1).size() == 0) false else true
    }

    private def isEvent(id: String, user: User): Boolean = {
        val query: Imports.DBObject = MongoDBObject("_id" -> id, "user.id" -> user.id)
        return if (collection.find(query).limit(1).size() == 0) false else true
    }

    private def initialParticipants(user: User): MongoDBList = {
        val builder: MongoDBListBuilder = MongoDBList.newBuilder[DBObject]
        builder += User.toParticipantDocument(user)
        return builder.result()
    }

    // DB document objects ===========================================================================================//

    private class DBEvent(user: User, event: Event) extends BasicDBObject {
        put("_id", java.util.UUID.randomUUID.toString)
        put("user", UserStorageService.userToPublicDocument(user))
        put("timestamp", event.timestamp)
        put("headline", event.headline)
        put("description", event.description)
        put("participants", initialParticipants(user))
        put("comments", new MongoDBList())
        put(COMMENTS_COUNT, 0)
        put("loc", new DBGeoPoint(event.x, event.y))
        put("tags", event.tags)
        put("distance", event.distance)
        put("pace", event.pace)
        put("deleted", false)
        put("spots", 1)
        put(UPDATED_AT, new Date().getTime)
    }

    private class DBEventComment(user: User, msg: String) extends BasicDBObject {
        put("msg", msg)
        put("date", new Date().getTime)
        put("user_id", user.id)
        put("user_name", user.first_name + " " + user.last_name)
        put("photo_url", user.photo_url)
    }

    //================================================================================================================//
}
